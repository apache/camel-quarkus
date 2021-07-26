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
package org.apache.camel.quarkus.component.vertx.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class VertxTest {

    //@Test
    public void testVertxComponent() {
        // Verify that the Vertx instance set up by the Quarkus extension
        // is the one used in the Camel component
        RestAssured.given()
                .get("/vertx/verify/instance")
                .then()
                .statusCode(200)
                .body(is("true"));

        String message = "Camel Quarkus Vert.x";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/vertx/post")
                .then()
                .statusCode(201)
                .body(is("Hello " + message));
    }
}
