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
package org.apache.camel.quarkus.component.servlet;

import java.nio.charset.StandardCharsets;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsEqual.equalTo;

@QuarkusTest
public class CamelServletTest {
    @Test
    public void defaultConfiguration() {
        RestAssured.get("/debug/configuration")
                .then()
                .body(
                        "isAsync", equalTo(false),
                        "threadName", startsWith("executor-thread"),
                        "initParams", anEmptyMap(),
                        "loadOnStartup", nullValue());
    }

    @Test
    public void multiplePaths() {
        RestAssured.get("/folder-1/rest-get").then().body(equalTo("GET: /rest-get"));
        RestAssured.get("/folder-2/rest-get").then().body(equalTo("GET: /rest-get"));
        RestAssured.post("/folder-1/rest-post").then().body(equalTo("POST: /rest-post"));
        RestAssured.post("/folder-2/rest-post").then().body(equalTo("POST: /rest-post"));
        RestAssured.get("/folder-1/hello").then().body(equalTo("GET: /hello"));
        RestAssured.get("/folder-2/hello").then().body(equalTo("GET: /hello"));
    }

    @Test
    public void namedWithServletClass() {
        RestAssured.get("/my-custom-folder/custom")
                .then()
                .body(equalTo("GET: /custom"))
                .and()
                .header("x-servlet-class-name", CustomServlet.class.getName());
    }

    @Test
    public void ignoredKey() {
        RestAssured.get("/my-named-folder/named")
                .then()
                .body(equalTo("GET: /my-named-servlet"));
    }

    @Test
    public void multipartDefaultConfig() {
        String body = "Hello World";
        RestAssured.given()
                .multiPart("file", "file", body.getBytes(StandardCharsets.UTF_8))
                .post("/folder-1/multipart/default")
                .then()
                .statusCode(200)
                .body(is(body));
    }

    @Test
    public void multipartCustomConfig() {
        String body = "Hello World";
        RestAssured.given()
                .multiPart("file", "file", body.getBytes(StandardCharsets.UTF_8))
                .post("/folder-1/multipart/default")
                .then()
                .statusCode(200)
                .body(is(body));

        // Request body exceeding the limits defined on the multipart config
        RestAssured.given()
                .multiPart("test-multipart", "file", body.repeat(10).getBytes(StandardCharsets.UTF_8))
                .post("/multipart-servlet/multipart")
                .then()
                // TODO: Expect 413 only - https://github.com/apache/camel-quarkus/issues/5830
                .statusCode(oneOf(413, 500));
    }

    @Test
    public void eagerInitServlet() {
        RestAssured.get("/eager-init-servlet/eager-init")
                .then()
                .body(
                        "isAsync", equalTo(false),
                        "threadName", startsWith("executor-thread"),
                        "initParams", anEmptyMap(),
                        "loadOnStartup", equalTo(1));
    }

    @Test
    public void asyncServlet() {
        RestAssured.get("/async-servlet/async")
                .then()
                .body(
                        "isAsync", equalTo(true),
                        "threadName", startsWith("executor-thread"),
                        "initParams.async", equalTo("true"),
                        "loadOnStartup", nullValue());
    }

    @Test
    public void asyncWithForceAwaitServlet() {
        RestAssured.get("/sync-async-servlet/force-await")
                .then()
                .body(
                        "isAsync", equalTo(true),
                        "threadName", startsWith("executor-thread"),
                        "initParams.async", equalTo("true"),
                        "initParams.forceAwait", equalTo("true"),
                        "loadOnStartup", nullValue());
    }

    @Test
    public void asyncWithCustomExecutor() {
        RestAssured.get("/custom-executor/execute/get")
                .then()
                .body(
                        "isAsync", equalTo(true),
                        "threadName", equalTo("custom-executor"),
                        "initParams.async", equalTo("true"),
                        "loadOnStartup", nullValue());
    }
}
