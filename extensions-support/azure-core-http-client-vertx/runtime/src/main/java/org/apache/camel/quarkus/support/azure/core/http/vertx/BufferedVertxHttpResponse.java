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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import io.vertx.core.buffer.Buffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class BufferedVertxHttpResponse extends VertxHttpAsyncResponse {

    private final Buffer body;

    BufferedVertxHttpResponse(HttpRequest request, io.vertx.ext.web.client.HttpResponse<Buffer> response, Buffer body) {
        super(request, response);
        this.body = body;
    }

    @Override
    public Flux<ByteBuffer> getBody() {
        return Flux.defer(() -> {
            if (this.body == null || this.body.length() == 0) {
                return Flux.empty();
            }
            return Flux.just(this.body.getByteBuf().nioBuffer());
        });
    }

    @Override
    public Mono<byte[]> getBodyAsByteArray() {
        return Mono.defer(() -> {
            if (this.body == null || this.body.length() == 0) {
                return Mono.empty();
            }
            return Mono.just(this.body.getBytes());
        });
    }

    @Override
    public Mono<InputStream> getBodyAsInputStream() {
        return Mono.defer(() -> {
            if (this.body == null || this.body.length() == 0) {
                return Mono.empty();
            }
            return Mono.just(new ByteArrayInputStream(this.body.getBytes()));
        });
    }

    @Override
    public HttpResponse buffer() {
        return this;
    }
}
