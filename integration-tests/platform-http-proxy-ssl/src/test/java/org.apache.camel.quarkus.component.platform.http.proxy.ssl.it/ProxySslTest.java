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
package org.apache.camel.quarkus.component.platform.http.proxy.ssl.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@QuarkusTestResource(PlatformHttpSSLTestResource.class)
public class ProxySslTest {
    @Test
    void test() {
        final var proxyUrl = "http://localhost:" + RestAssured.port;
        String url = given()
                .get("/platform-http-proxy-ssl")
                .body().asString();

        // forcing RestAssured to send a GET instead of CONNECT with proxy settings
        url = url.replace("https", "http");
        given()
                .proxy(proxyUrl)
                .contentType(ContentType.JSON)
                .when().get(url)
                .then()
                .statusCode(200)
                .body(equalTo("{\"message\": \"Hello World!\"}"));

        given()
                .body("hello")
                .proxy(proxyUrl)
                .contentType(ContentType.JSON)
                .when().post(url)
                .then()
                .statusCode(200)
                .body(equalTo("{\"message\": \"Hello World!\"}"));
    }
}
