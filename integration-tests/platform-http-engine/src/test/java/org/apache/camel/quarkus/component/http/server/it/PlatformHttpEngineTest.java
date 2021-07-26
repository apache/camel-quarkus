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
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.component.platform.http.vertx.VertxPlatformHttpEngine;
import org.junit.jupiter.api.Disabled;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class PlatformHttpEngineTest {
    //@Test
    public void registrySetUp() {
        RestAssured.given()
                .get("/test/registry/inspect")
                .then()
                .statusCode(200)
                .body(
                        PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME, is(VertxPlatformHttpEngine.class.getName()),
                        PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME, is(PlatformHttpComponent.class.getName()));
    }

    //@Test
    public void basic() {
        RestAssured.given()
                .get("/platform-http/hello")
                .then()
                .statusCode(200)
                .body(equalTo("platform-http/hello"));
    }

    @Disabled("https://github.com/quarkusio/quarkus/issues/4408")
    //@Test
    public void invalidMethod() {
        RestAssured.post("/platform-http/hello")
                .then().statusCode(405);
    }
}
