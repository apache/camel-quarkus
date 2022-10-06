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
package org.apache.camel.quarkus.support.azure.core.http.vertx.implementation;

import java.nio.ByteBuffer;

import com.azure.core.http.HttpRequest;
import com.azure.core.util.FluxUtil;
import io.vertx.core.http.HttpClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default HTTP response for Vert.x.
 */
public class VertxHttpAsyncResponse extends VertxHttpResponseBase {
    public VertxHttpAsyncResponse(HttpRequest azureHttpRequest, HttpClientResponse vertxHttpResponse) {
        super(azureHttpRequest, vertxHttpResponse);
        vertxHttpResponse.pause();
    }

    @Override
    public Flux<ByteBuffer> getBody() {
        return streamResponseBody();
    }

    @Override
    public Mono<byte[]> getBodyAsByteArray() {
        return FluxUtil.collectBytesFromNetworkResponse(streamResponseBody(), getHeaders())
                .flatMap(bytes -> (bytes == null || bytes.length == 0)
                        ? Mono.empty()
                        : Mono.just(bytes));
    }

    private Flux<ByteBuffer> streamResponseBody() {
        HttpClientResponse vertxHttpResponse = getVertxHttpResponse();
        return Flux.create(sink -> {
            vertxHttpResponse.handler(buffer -> {
                sink.next(buffer.getByteBuf().nioBuffer());
            }).endHandler(event -> {
                sink.complete();
            }).exceptionHandler(sink::error);

            vertxHttpResponse.resume();
        });
    }
}
