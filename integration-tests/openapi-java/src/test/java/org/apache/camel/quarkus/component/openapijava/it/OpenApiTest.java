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
package org.apache.camel.quarkus.component.openapijava.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class OpenApiTest {
    //@Test
    public void invokeApiEndpoint() {
        RestAssured.given()
                .get("/fruits/list")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "description", containsInAnyOrder("Winter fruit", "Tropical fruit"),
                        "name", containsInAnyOrder("Apple", "Pineapple"));
    }

    //@Test
    public void invokeApiDocumentEndpoint() {
        RestAssured.given()
                .get("/openapi.json")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "paths.'/fruits/list'", hasKey("get"),
                        "paths.'/fruits/list'.get.operationId", is("list"));
    }

}
