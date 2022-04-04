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

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

import static org.apache.camel.quarkus.support.azure.core.http.vertx.VertxHttpClientTests.RETURN_HEADERS_AS_IS_PATH;

/**
 * Mock response transformer used to test {@link VertxHttpClient}.
 */
public class VertxHttpClientResponseTransformer extends ResponseTransformer {
    public static final String NAME = "vertx-http-client-response-transformer";

    @Override
    public Response transform(Request request, Response response, FileSource fileSource, Parameters parameters) {
        String url = request.getUrl();

        if (RETURN_HEADERS_AS_IS_PATH.equalsIgnoreCase(url)) {
            return Response.response()
                    .status(200)
                    .headers(request.getHeaders())
                    .build();
        }

        return response;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }
}
