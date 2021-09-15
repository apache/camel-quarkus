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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_ADMIN;
import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_ADMIN_PASSWORD;
import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_NO_ADMIN;
import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_NO_ADMIN_PASSWORD;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@QuarkusTestResource(HttpTestResource.class)
class HttpTest {

    private static final String[] HTTP_COMPONENTS = new String[] { "ahc", "http", "netty-http", "vertx-http" };

    @ParameterizedTest
    @MethodSource("getHttpComponentNames")
    public void basicProducer(String component) {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .when()
                .get("/test/client/{component}/get", component)
                .then()
                .statusCode(200)
                .body(is("get"));

        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .body("message")
                .when()
                .post("/test/client/{component}/post", component)
                .then()
                .statusCode(200)
                .body(is("MESSAGE"));
    }

    @ParameterizedTest
    @MethodSource("getHttpComponentNames")
    public void httpsProducer(String component) {
        final int port = ConfigProvider.getConfig().getValue("camel.netty-http.https-test-port", Integer.class);

        RestAssured
                .given()
                .queryParam("test-port", port)
                .when()
                .get("/test/client/{component}/get-https", component)
                .then()
                .statusCode(200)
                .body(containsString("Czech Republic"));
    }

    @ParameterizedTest
    @MethodSource("getHttpComponentNames")
    public void basicAuth(String component) {
        // No credentials expect 401 response
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .queryParam("component", component)
                .when()
                .get("/test/client/{component}/auth/basic", component)
                .then()
                .statusCode(401);

        // Invalid credentials expect 403 response
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .queryParam("component", component)
                .queryParam("username", USER_NO_ADMIN)
                .queryParam("password", USER_NO_ADMIN_PASSWORD)
                .when()
                .get("/test/client/{component}/auth/basic", component)
                .then()
                .statusCode(403);

        // Valid credentials expect 200 response
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .queryParam("component", component)
                .queryParam("username", USER_ADMIN)
                .queryParam("password", USER_ADMIN_PASSWORD)
                .when()
                .get("/test/client/{component}/auth/basic", component)
                .then()
                .statusCode(200)
                .body(is("Component " + component + " is using basic auth"));
    }

    @Test
    public void basicAuthCache() {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .when()
                .get("/test/client/http/auth/basic/cache")
                .then()
                .statusCode(200)
                .body(is("Component http is using basic auth"));
    }

    @ParameterizedTest
    @MethodSource("getHttpComponentNames")
    public void proxyServer(String component) {
        RestAssured
                .given()
                .when()
                .get("/test/client/{component}/proxy", component)
                .then()
                .statusCode(200)
                .body(
                        "metadata.groupId", is("org.apache.camel.quarkus"),
                        "metadata.artifactId", is("camel-quarkus-" + component));
    }

    @ParameterizedTest
    @MethodSource("getHttpComponentNames")
    public void compression(String component) {
        final int port = ConfigProvider.getConfig().getValue("camel.netty-http.compression-test-port", Integer.class);
        RestAssured
                .given()
                .queryParam("test-port", port)
                .when()
                .get("/test/client/{component}/compression", component)
                .then()
                .statusCode(200)
                .body(is("Netty Hello World Compressed"));
    }

    @ParameterizedTest
    @MethodSource("getHttpComponentNames")
    public void transferException(String component) {
        final int port = ConfigProvider.getConfig().getValue("camel.netty-http.test-port", Integer.class);
        RestAssured
                .given()
                .queryParam("test-port", port)
                .when()
                .get("/test/client/{component}/serialized/exception", component)
                .then()
                .statusCode(200)
                .body(is("java.lang.IllegalStateException"));
    }

    @Test
    public void basicNettyHttpServer() {
        final int port = ConfigProvider.getConfig().getValue("camel.netty-http.test-port", Integer.class);
        RestAssured
                .given()
                .port(port)
                .when()
                .get("/test/server/hello")
                .then()
                .statusCode(200)
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
                .get("/test/client/send-dynamic")
                .then()
                .statusCode(200)
                .body(
                        "q", is(not(empty())),
                        "fq", is(not(empty())));
    }

    @Test
    public void serviceCall() {
        final int port = ConfigProvider.getConfig().getValue("camel.netty-http.test-port", Integer.class);
        RestAssured
                .given()
                .port(port)
                .when()
                .get("/test/server/serviceCall")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello from myService"));
    }

    private static String[] getHttpComponentNames() {
        return HTTP_COMPONENTS;
    }
}
