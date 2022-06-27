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
package org.apache.camel.quarkus.component.http.server.it;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class PlatformHttpTest {

    @BeforeAll
    public static void beforeAll() {
        RestAssured.trustStore("truststore.p12", "s3cr3t");
    }

    @Test
    public void basic() {
        RestAssured.given()
                .param("name", "Kermit")
                .get("/platform-http/hello")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Kermit"));

        RestAssured.given()
                .body("Camel")
                .post("/platform-http/get-post")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Camel"));

        RestAssured.given()
                .get("/platform-http/get-post")
                .then()
                .statusCode(200)
                .body(equalTo("Hello ")); // there is no body for get
    }

    @Test
    public void rest() throws Throwable {
        RestAssured.get("/my-context/platform-http/rest-get")
                .then().body(equalTo("GET: /my-context/platform-http/rest-get"));
        RestAssured.given()
                .contentType("text/plain")
                .post("/my-context/platform-http/rest-post")
                .then().body(equalTo("POST: /my-context/platform-http/rest-post"));
    }

    @Test
    public void consumes() throws Throwable {
        RestAssured.given()
                .contentType("application/json")
                .post("/my-context/platform-http/rest-post")
                .then()
                .statusCode(415);

        RestAssured.given()
                .contentType("text/plain")
                .post("/my-context/platform-http/rest-post")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType("application/json")
                .post("/platform-http/consumes")
                .then()
                .statusCode(415);

        RestAssured.given()
                .contentType("text/plain")
                .post("/platform-http/consumes")
                .then()
                .statusCode(200);
    }

    @Test
    public void produces() throws Throwable {
        RestAssured.given()
                .accept("application/json")
                .contentType("text/plain")
                .post("/my-context/platform-http/rest-post")
                .then()
                .statusCode(406);

        RestAssured.given()
                .accept("text/plain")
                .contentType("text/plain")
                .post("/my-context/platform-http/rest-post")
                .then()
                .statusCode(200);

        RestAssured.given()
                .accept("application/json")
                .contentType("text/plain")
                .post("/platform-http/produces")
                .then()
                .statusCode(406);

        RestAssured.given()
                .accept("text/plain")
                .contentType("text/plain")
                .post("/platform-http/produces")
                .then()
                .statusCode(200);
    }

    @Test
    public void invalidMethod() {
        RestAssured.post("/platform-http/hello")
                .then().statusCode(405);
        RestAssured.post("/my-context/platform-http/rest-get")
                .then().statusCode(405);
        RestAssured.get("/my-context/platform-http/rest-post")
                .then().statusCode(405);
    }

    @Test
    public void multipart() {
        final byte[] bytes = new byte[] { 0xc, 0x0, 0xf, 0xe, 0xb, 0xa, 0xb, 0xe };
        final byte[] returnedBytes = RestAssured.given().contentType("multipart/form-data")
                .multiPart("file", "bytes.bin", bytes)
                .formParam("description", "cofe babe")
                .post("/platform-http/multipart")
                .then()
                .statusCode(200)
                .extract().body().asByteArray();
        Assertions.assertArrayEquals(bytes, returnedBytes);
    }

    @Test
    public void formUrlEncoded() {
        RestAssured.given().contentType("application/x-www-form-urlencoded")
                .formParam("k1", "v1")
                .formParam("k2", "v2")
                .post("/platform-http/form-urlencoded")
                .then()
                .statusCode(200)
                .body(equalTo("k1=V1\nk2=V2"));
    }

    @Test
    public void customHeaderFilterStrategy() {
        RestAssured.given()
                .queryParam("k1", "v1")
                .queryParam("k2", "v2")
                .get("/platform-http/header-filter-strategy")
                .then()
                .statusCode(200)
                .body(equalTo("k1=\nk2=v2")); // k1 filtered out by TestHeaderFilterStrategy
    }

    @Test
    public void multiValueParams() {
        RestAssured.given()
                .queryParam("k1", "v1")
                .queryParam("k1", "v2")
                .get("/platform-http/multi-value-params")
                .then()
                .statusCode(200)
                .body(equalTo("k1=[v1, v2]"));
    }

    @Test
    public void encoding() throws UnsupportedEncodingException {
        final String outgoingEncoding = "ISO-8859-2";
        final String bodyText = "Ťava dvojhrbá"; // Camelus bactrianus in Slovak
        final byte[] returnedBytes = RestAssured.given()
                .contentType("text/plain; charset=" + outgoingEncoding)
                .body(bodyText.getBytes(outgoingEncoding))
                .post("/platform-http/encoding")
                .then()
                .statusCode(200)
                .extract().body().asByteArray();
        Assertions.assertArrayEquals(bodyText.getBytes(StandardCharsets.UTF_8), returnedBytes);
    }

    @Test
    public void responseCodeViaHeader() throws UnsupportedEncodingException {
        RestAssured.given()
                .get("/platform-http/response-code-299")
                .then()
                .statusCode(299);
    }

    @Test
    public void code204Null() throws Exception {
        RestAssured.given()
                .get("/platform-http/null-body")
                .then()
                .statusCode(204);
    }

    @Test
    public void code204EmptyString() throws Exception {
        RestAssured.given()
                .get("/platform-http/empty-string-body")
                .then()
                .statusCode(204);
    }

    @Test
    public void code204SomeString() throws Exception {
        RestAssured.given()
                .get("/platform-http/some-string")
                .then()
                .statusCode(200)
                .body(equalTo("No Content"));
    }

    @Test
    public void code200EmptyString() throws Exception {
        RestAssured.given()
                .get("/platform-http/empty-string-200")
                .then()
                .statusCode(200)
                .body(equalTo(""));
    }

    @Test
    public void pathParam() throws Exception {
        RestAssured.given()
                .get("/my-context/platform-http/hello-by-name/Kermit")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Kermit"));
    }

    @Test
    public void log() {
        String message = "Camel Quarkus Platform HTTP";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .when()
                .post("/platform-http/log")
                .then()
                .statusCode(200)
                .body(equalTo(message));
    }

    @ParameterizedTest
    @MethodSource("httpMethods")
    public void methods(Method method) {
        String expected = "";
        if (!method.equals(Method.HEAD)) {
            expected = "Hello " + method.name();
        }

        RestAssured.given()
                .request(method, "/platform-http/allmethods")
                .then()
                .statusCode(200)
                .body(equalTo(expected));
    }

    @Test
    public void pathPrefix() {
        // Base part of the path should return 404
        final String basePath = "/platform-http/path";
        RestAssured.given()
                .get(basePath)
                .then()
                .statusCode(404);

        // Anything with the expected path prefix should be valid
        String path = basePath + "/prefix";
        for (int i = 0; i < 5; i++) {
            RestAssured.given()
                    .get(path)
                    .then()
                    .statusCode(200)
                    .body(equalTo("Hello " + path));
            path += "/" + i;
        }
    }

    @Test
    public void testWebhook() throws InterruptedException {
        String path = RestAssured
                .given()
                .get("/platform-http/webhookpath")
                .then()
                .extract()
                .body()
                .asString();

        RestAssured.given()
                .urlEncodingEnabled(false)
                .post("/my-context" + path)
                .then()
                .statusCode(200)
                .body(equalTo("Hello Camel Quarkus Webhook"));
    }

    @Test
    public void testPathSecuredWithBasicAuth() {
        // No credentials
        RestAssured.given()
                .when()
                .get("/platform-http/secure/basic")
                .then()
                .statusCode(401);

        // Invalid credentials
        RestAssured.given()
                .auth()
                .basic("camel", "s3cr3t")
                .get("/platform-http/secure/basic")
                .then()
                .statusCode(401);

        // Valid credentials
        RestAssured.given()
                .auth()
                .basic("camel", "p4ssw0rd")
                .get("/platform-http/secure/basic")
                .then()
                .statusCode(200)
                .header("Authorization", notNullValue())
                .body(equalTo("camel:Admin"));
    }

    @Test
    public void registrySetUp() {
        RestAssured.given()
                .get("/registry/inspect")
                .then()
                .statusCode(200)
                .body(
                        "engine", endsWith("CamelQuarkusVertxPlatformHttpEngine"),
                        "component", is(PlatformHttpComponent.class.getName()));
    }

    private static Method[] httpMethods() {
        return Method.values();
    }
}
