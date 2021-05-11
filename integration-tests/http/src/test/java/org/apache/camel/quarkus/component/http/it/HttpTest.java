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
package org.apache.camel.quarkus.component.http.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@QuarkusTestResource(HttpTestResource.class)
class HttpTest {
    @ParameterizedTest
    @ValueSource(strings = { "ahc", "http", "netty-http", "vertx-http" })
    public void basicProducer(String component) {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .when()
                .get("/test/client/{component}/get", component)
                .then()
                .body(is("get"));

        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .body("message")
                .when()
                .post("/test/client/{component}/post", component)
                .then()
                .body(is("MESSAGE"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "ahc", "http", "netty-http", "vertx-http" })
    public void httpsProducer(String component) {
        final int port = ConfigProvider.getConfig().getValue("camel.netty-http.https-test-port", Integer.class);

        RestAssured
                .given()
                .queryParam("test-port", port)
                .when()
                .get("/test/client/{component}/get-https", component)
                .then()
                .body(containsString("Czech Republic"));
    }

    @Test
    public void basicNettyHttpServer() throws Exception {
        final int port = ConfigProvider.getConfig().getValue("camel.netty-http.test-port", Integer.class);

        RestAssured
                .given()
                .port(port)
                .when()
                .get("/test/server/hello")
                .then()
                .body(is("Netty Hello World"));
    }

    @Test
    public void testAhcWsProducerConsumer() {
        String body = "Camel Quarkus AHC-WS";
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .body(body)
                .post("/test/client/ahc-ws/post")
                .then()
                .body(is("Hello " + body));
    }

    @Test
    public void sendDynamic() {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .accept(ContentType.JSON)
                .when()
                .get("/test/send-dynamic")
                .then()
                .body(
                        "q", is(not(empty())),
                        "fq", is(not(empty())));
    }
}
