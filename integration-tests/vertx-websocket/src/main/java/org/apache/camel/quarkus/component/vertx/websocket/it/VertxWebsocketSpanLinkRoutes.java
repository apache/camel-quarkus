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
package org.apache.camel.quarkus.component.vertx.websocket.it;

import org.apache.camel.builder.RouteBuilder;

/**
 * Routes for testing OpenTelemetry span links with WebSocket connections.
 * This test scenario validates that WebSocket message spans are linked
 * back to the HTTP upgrade request span.
 */
public class VertxWebsocketSpanLinkRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // Route 1: WebSocket Consumer - receives messages from client
        from("vertx-websocket:/span-link-test")
                .routeId("websocket-consumer")
                .to("direct:logMessage");

        // Route 2: Log the message
        from("direct:logMessage")
                .routeId("log-message")
                .log("Greeting: ${body}");

        // Route 3: Timer Producer - sends messages to WebSocket clients (with sendToAll=true)
        from("timer:websocket-timer?period=5000&repeatCount=1")
                .routeId("timer-producer")
                .setBody().constant("Hello World")
                .to("vertx-websocket:/span-link-test?sendToAll=true");
    }
}
