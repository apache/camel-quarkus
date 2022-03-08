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

import java.io.Closeable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Objects;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.Context;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link HttpClient} implementation for the Vert.x {@link WebClient}.
 */
public class VertxHttpClient implements HttpClient, Closeable {

    private final WebClient client;
    private final WebClientOptions options;

    public VertxHttpClient(WebClient client, WebClientOptions options) {
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(client, "options cannot be null");
        this.client = client;
        this.options = options;
    }

    @Override
    public Mono<HttpResponse> send(HttpRequest request) {
        return send(request, Context.NONE);
    }

    @Override
    public Mono<HttpResponse> send(HttpRequest request, Context context) {
        boolean eagerlyReadResponse = (boolean) context.getData("azure-eagerly-read-response").orElse(false);
        return Mono.create(sink -> sink.onRequest(value -> {
            toVertxHttpRequest(request).subscribe(vertxHttpRequest -> {
                vertxHttpRequest.send(new VertxHttpResponseHandler(request, sink, eagerlyReadResponse));
            }, sink::error);
        }));
    }

    public void close() {
        this.client.close();
    }

    // Exposed for testing
    public WebClientOptions getWebClientOptions() {
        return options;
    }

    private Mono<VertxHttpRequest> toVertxHttpRequest(HttpRequest request) {
        return Mono.from(convertBodyToBuffer(request))
                .map(buffer -> {
                    HttpMethod httpMethod = request.getHttpMethod();
                    io.vertx.core.http.HttpMethod requestMethod = io.vertx.core.http.HttpMethod.valueOf(httpMethod.name());

                    URL url = request.getUrl();
                    if (url.getPath().isEmpty()) {
                        try {
                            // Azure API documentation states:
                            //
                            // The URI must always include the forward slash (/) to separate the host name
                            // from the path and query portions of the URI.
                            //
                            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/" + url.getFile());
                        } catch (MalformedURLException e) {
                            throw new IllegalStateException(e);
                        }
                    }

                    io.vertx.ext.web.client.HttpRequest<Buffer> delegate = client
                            .requestAbs(requestMethod, url.toString());

                    if (request.getHeaders() != null) {
                        request.getHeaders()
                                .stream()
                                .forEach(httpHeader -> delegate.putHeader(httpHeader.getName(),
                                        httpHeader.getValuesList()));
                    }

                    return new VertxHttpRequest(delegate, buffer);
                });
    }

    private Mono<Buffer> convertBodyToBuffer(HttpRequest request) {
        return Mono.using(() -> Buffer.buffer(),
                buffer -> getBody(request).reduce(buffer, (b, byteBuffer) -> {
                    for (int i = 0; i < byteBuffer.limit(); i++) {
                        b.appendByte(byteBuffer.get(i));
                    }
                    return b;
                }), buffer -> buffer.getClass());
    }

    private Flux<ByteBuffer> getBody(HttpRequest request) {
        long contentLength = 0;
        String contentLengthHeader = request.getHeaders().getValue("content-length");
        if (contentLengthHeader != null) {
            contentLength = Long.parseLong(contentLengthHeader);
        }

        Flux<ByteBuffer> body = request.getBody();
        if (body == null || contentLength <= 0) {
            body = Flux.just(Buffer.buffer().getByteBuf().nioBuffer());
        }

        return body;
    }
}
