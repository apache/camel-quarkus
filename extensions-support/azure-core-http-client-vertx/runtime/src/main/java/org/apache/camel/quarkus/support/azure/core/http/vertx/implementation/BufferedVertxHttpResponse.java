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
import com.azure.core.http.HttpResponse;
import com.azure.core.util.BinaryData;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class BufferedVertxHttpResponse extends VertxHttpAsyncResponse {
    private final Buffer body;

    public BufferedVertxHttpResponse(HttpRequest azureHttpRequest, HttpClientResponse vertxHttpResponse, Buffer body) {
        super(azureHttpRequest, vertxHttpResponse);
        this.body = body;
    }

    @Override
    public BinaryData getBodyAsBinaryData() {
        return BinaryData.fromBytes(body.getBytes());
    }

    @Override
    public Flux<ByteBuffer> getBody() {
        return Flux.defer(() -> {
            if (this.body.length() == 0) {
                return Flux.empty();
            }
            return Flux.just(ByteBuffer.wrap(this.body.getBytes()));
        });
    }

    @Override
    public Mono<byte[]> getBodyAsByteArray() {
        return Mono.defer(() -> {
            if (this.body.length() == 0) {
                return Mono.empty();
            }
            return Mono.just(this.body.getBytes());
        });
    }

    @Override
    public HttpResponse buffer() {
        return this;
    }
}
