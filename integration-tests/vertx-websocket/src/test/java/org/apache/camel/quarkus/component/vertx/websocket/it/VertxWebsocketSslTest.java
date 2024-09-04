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

import java.net.URI;
import java.util.List;

import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class VertxWebsocketSslTest {
    @TestHTTPResource(value = "/", tls = true)
    URI root;

    @BeforeAll
    public static void beforeAll() {
        RestAssured.trustStore("target/certs/vertx-websocket-truststore.p12", "changeit");
    }

    @Test
    public void ssl() throws Exception {
        URI uri = URI.create(root.toString().replace("https", "wss"));
        String message = "SSL Vert.x WebSocket Route";

        RestAssured.given()
                .queryParam("hostPort", "localhost:8441")
                .get("/vertx-websocket/invalid/consumer/uri")
                .then()
                .statusCode(500)
                .body(matchesPattern(
                        "Invalid host/port localhost:8441.*can only be configured as (localhost|0.0.0.0):" + uri.getPort()));

        try (VertxWebsocketTest.WebSocketConnection connection = new VertxWebsocketTest.WebSocketConnection(uri, null)) {
            connection.connect();

            RestAssured.given()
                    .queryParam("endpointUri",
                            "vertx-websocket:/?sendToAll=true&sslContextParameters=#clientSSLContextParameters")
                    .body(message)
                    .post("/vertx-websocket/run")
                    .then()
                    .statusCode(204);

            List<String> messages = connection.getMessages();
            assertEquals(1, messages.size());
            assertEquals(message, messages.get(0));
        }
    }
}
