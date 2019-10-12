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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.quarkus.component.platform.http.runtime.QuarkusPlatformHttpEngine;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
class PlatformHttpTest {

    @Test
    public void registrySetUp() {
        JsonPath p = RestAssured.given()
            .get("/test/registry/inspect")
            .then()
                .statusCode(200)
                .extract()
                    .body()
                    .jsonPath();

        assertThat(p.getString(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME)).isEqualTo(QuarkusPlatformHttpEngine.class.getName());
        assertThat(p.getString(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME)).isEqualTo(PlatformHttpComponent.class.getName());
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
        RestAssured.get("/platform-http/rest-get")
            .then().body(equalTo("GET: /rest-get"));
        RestAssured.post("/platform-http/rest-post")
            .then().body(equalTo("POST: /rest-post"));
    }

    @Disabled("See https://github.com/quarkusio/quarkus/issues/4408")
    @Test
    public void invalidMethod() {
        RestAssured.post("/platform-http/hello")
            .then().statusCode(405);
        RestAssured.post("/platform-http/rest-get")
            .then().statusCode(405);
        RestAssured.get("/platform-http/rest-post")
            .then().statusCode(405);
    }

}
