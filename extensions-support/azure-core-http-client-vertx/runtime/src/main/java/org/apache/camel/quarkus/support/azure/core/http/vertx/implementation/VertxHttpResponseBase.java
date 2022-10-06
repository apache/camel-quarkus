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

import java.nio.charset.Charset;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.CoreUtils;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientResponse;
import reactor.core.publisher.Mono;

abstract class VertxHttpResponseBase extends HttpResponse {

    private final HttpClientResponse vertxHttpResponse;
    private final HttpHeaders headers;

    VertxHttpResponseBase(HttpRequest azureHttpRequest, HttpClientResponse vertxHttpResponse) {
        super(azureHttpRequest);
        this.vertxHttpResponse = vertxHttpResponse;
        this.headers = fromVertxHttpHeaders(vertxHttpResponse.headers());
    }

    private HttpHeaders fromVertxHttpHeaders(MultiMap headers) {
        HttpHeaders azureHeaders = new HttpHeaders();
        headers.names().forEach(name -> azureHeaders.set(name, headers.getAll(name)));
        return azureHeaders;
    }

    protected HttpClientResponse getVertxHttpResponse() {
        return this.vertxHttpResponse;
    }

    @Override
    public int getStatusCode() {
        return this.vertxHttpResponse.statusCode();
    }

    @Override
    public String getHeaderValue(String name) {
        return this.headers.getValue(name);
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.headers;
    }

    @Override
    public final Mono<String> getBodyAsString() {
        return getBodyAsByteArray().map(bytes -> CoreUtils.bomAwareToString(bytes, getHeaderValue("Content-Type")));
    }

    @Override
    public final Mono<String> getBodyAsString(Charset charset) {
        return getBodyAsByteArray().map(bytes -> CoreUtils.bomAwareToString(bytes, charset.toString()));
    }
}
