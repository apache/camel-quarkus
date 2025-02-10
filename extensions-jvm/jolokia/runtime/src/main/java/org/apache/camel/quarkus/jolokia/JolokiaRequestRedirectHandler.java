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
package org.apache.camel.quarkus.jolokia;

import java.net.URI;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Vert.x route to redirect from /q/jolokia to the Jolokia embedded HTTP server endpoint
 */
final class JolokiaRequestRedirectHandler implements Handler<RoutingContext> {
    private final String jolokiaAgentBaseUrl;
    private final String jolokiaManagementEndpointPath;

    public JolokiaRequestRedirectHandler(URI jolokiaAgentBaseUrl, String jolokiaManagementEndpointPath) {
        this.jolokiaManagementEndpointPath = jolokiaManagementEndpointPath;
        this.jolokiaAgentBaseUrl = jolokiaAgentBaseUrl.toString();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        String uri = routingContext.request().uri();
        routingContext.response()
                .setStatusCode(301)
                .putHeader("Location", jolokiaAgentBaseUrl + uri.replace(jolokiaManagementEndpointPath, ""))
                .end();
    }
}
