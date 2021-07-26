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
package org.apache.camel.quarkus.component.dozer.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class DozerTest {

    //@Test
    public void testDozerTypeConverter() {
        RestAssured.get("/dozer/map/using/converter")
                .then()
                .statusCode(200)
                .body("address", notNullValue(),
                        "address.zip", is("12345"),
                        "address.street", is("Camel Street"));
    }

    //@Test
    public void testDozerEndpoint() {
        RestAssured.get("/dozer/map")
                .then()
                .statusCode(200)
                .body("address", notNullValue(),
                        "address.zip", is("12345"),
                        "address.street", is("Camel Street"),
                        "created", containsString("1990"),
                        "internalFileAsString", is("/test"),
                        "internalClassAsString", is("java.lang.String"),
                        "internalUrl", is("http://customer"),
                        "internal.text", is("hello internal"));
    }

    //@Test
    public void testDozerVariableMapper() {
        RestAssured.get("/dozer/map/using/variable")
                .then()
                .statusCode(200)
                .body("firstName", is("Camel"),
                        "lastName", is("Quarkus"));
    }

    //@Test
    public void testDozerExpressionMapper() {
        RestAssured.get("/dozer/map/using/expression")
                .then()
                .statusCode(200)
                .body("firstName", is("Camel"),
                        "lastName", is("Quarkus"));
    }
}
