/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.support.azure.core.http.vertx;

import java.nio.ByteBuffer;
import java.util.Objects;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.Context;
import com.azure.core.util.Contexts;
import com.azure.core.util.ProgressReporter;
import io.netty.buffer.Unpooled;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.RequestOptions;
import org.apache.camel.quarkus.support.azure.core.http.vertx.implementation.BufferedVertxHttpResponse;
import org.apache.camel.quarkus.support.azure.core.http.vertx.implementation.VertxHttpAsyncResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * {@link HttpClient} implementation for the Vert.x {@link io.vertx.core.http.HttpClient}.
 */
class VertxAsyncHttpClient implements HttpClient {
    private final Scheduler scheduler;
    final io.vertx.core.http.HttpClient client;

    /**
     * Constructs a {@link VertxAsyncHttpClient}.
     *
     * @param client The Vert.x {@link io.vertx.core.http.HttpClient}
     */
    VertxAsyncHttpClient(io.vertx.core.http.HttpClient client, Vertx vertx) {
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(vertx, "vertx cannot be null");
        this.client = client;
        this.scheduler = Schedulers.fromExecutor(vertx.nettyEventLoopGroup());
    }

    @Override
    public Mono<HttpResponse> send(HttpRequest request) {
        return send(request, Context.NONE);
    }

    @Override
    public Mono<HttpResponse> send(HttpRequest request, Context context) {
        boolean eagerlyReadResponse = (boolean) context.getData("azure-eagerly-read-response").orElse(false);
        ProgressReporter progressReporter = Contexts.with(context).getHttpRequestProgressReporter();
        return Mono.create(sink -> toVertxHttpRequest(request).subscribe(vertxHttpRequest -> {
            vertxHttpRequest.exceptionHandler(sink::error);

            HttpHeaders requestHeaders = request.getHeaders();
            if (requestHeaders != null) {
                requestHeaders.stream().forEach(header -> vertxHttpRequest.putHeader(header.getName(), header.getValuesList()));
                if (request.getHeaders().get("Content-Length") == null) {
                    vertxHttpRequest.setChunked(true);
                }
            } else {
                vertxHttpRequest.setChunked(true);
            }

            vertxHttpRequest.response(event -> {
                if (event.succeeded()) {
                    HttpClientResponse vertxHttpResponse = event.result();
                    vertxHttpResponse.exceptionHandler(sink::error);

                    if (eagerlyReadResponse) {
                        vertxHttpResponse.body(bodyEvent -> {
                            if (bodyEvent.succeeded()) {
                                sink.success(new BufferedVertxHttpResponse(request, vertxHttpResponse,
                                        bodyEvent.result()));
                            } else {
                                sink.error(bodyEvent.cause());
                            }
                        });
                    } else {
                        sink.success(new VertxHttpAsyncResponse(request, vertxHttpResponse));
                    }
                } else {
                    sink.error(event.cause());
                }
            });

            getRequestBody(request, progressReporter)
                    .subscribeOn(scheduler)
                    .map(Unpooled::wrappedBuffer)
                    .map(Buffer::buffer)
                    .subscribe(vertxHttpRequest::write, sink::error, vertxHttpRequest::end);
        }, sink::error));
    }

    private Mono<HttpClientRequest> toVertxHttpRequest(HttpRequest request) {
        HttpMethod httpMethod = request.getHttpMethod();
        io.vertx.core.http.HttpMethod requestMethod = io.vertx.core.http.HttpMethod.valueOf(httpMethod.name());

        RequestOptions options = new RequestOptions();
        options.setMethod(requestMethod);
        options.setAbsoluteURI(request.getUrl());
        return Mono.fromCompletionStage(client.request(options).toCompletionStage());
    }

    private Flux<ByteBuffer> getRequestBody(HttpRequest request, ProgressReporter progressReporter) {
        Flux<ByteBuffer> body = request.getBody();
        if (body == null) {
            return Flux.empty();
        }

        if (progressReporter != null) {
            body = body.map(buffer -> {
                progressReporter.reportProgress(buffer.remaining());
                return buffer;
            });
        }

        return body;
    }
}
