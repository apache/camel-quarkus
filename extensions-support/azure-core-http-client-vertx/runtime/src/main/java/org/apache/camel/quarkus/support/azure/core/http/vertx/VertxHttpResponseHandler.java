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

import com.azure.core.http.HttpRequest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import reactor.core.publisher.MonoSink;

/**
 * {@link Handler} for Azure HTTP responses.
 */
class VertxHttpResponseHandler implements Handler<AsyncResult<HttpResponse<Buffer>>> {

    private final HttpRequest request;
    private final MonoSink<com.azure.core.http.HttpResponse> sink;
    private final boolean eagerlyReadResponse;

    VertxHttpResponseHandler(HttpRequest request, MonoSink<com.azure.core.http.HttpResponse> sink,
            boolean eagerlyReadResponse) {
        this.request = request;
        this.sink = sink;
        this.eagerlyReadResponse = eagerlyReadResponse;
    }

    @Override
    public void handle(AsyncResult<HttpResponse<Buffer>> event) {
        if (event.succeeded()) {
            VertxHttpResponse response;
            if (eagerlyReadResponse) {
                io.vertx.ext.web.client.HttpResponse<Buffer> originalResponse = event.result();
                response = new BufferedVertxHttpResponse(request, originalResponse, originalResponse.body());
            } else {
                response = new VertxHttpAsyncResponse(request, event.result());
            }
            sink.success(response);
        } else {
            if (event.cause() != null) {
                sink.error(event.cause());
            }
        }
    }
}
