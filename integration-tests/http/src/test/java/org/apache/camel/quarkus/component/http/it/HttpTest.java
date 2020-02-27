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
import org.apache.camel.quarkus.test.TrustStoreResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(HttpTestResource.class)
@QuarkusTestResource(TrustStoreResource.class)
class HttpTest {
    @ParameterizedTest
    @ValueSource(strings = { "ahc", "http", "netty-http" })
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
    @ValueSource(strings = { "ahc",
            "http" /*, "netty-http" disabled because of https://github.com/apache/camel-quarkus/issues/695 */ })
    public void httpsProducer(String component) {
        RestAssured
                .given()
                .when()
                .get("/test/client/{component}/get-https", component)
                .then()
                .body(containsString("Czech Republic"));
    }

    @Test
    public void restcountries() throws Exception {
        RestAssured
                .given()
                .baseUri("https://restcountries.eu")
                .port(443)
                .when()
                .accept("application/json")
                .get("/rest/v2/alpha/cz")
                .then()
                .statusCode(200)
                .body(containsString("Czech Republic"));
    }

    @Test
    public void basicNettyHttpServer() throws Exception {
        final int port = Integer.getInteger("camel.netty-http.test-port");

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

}
