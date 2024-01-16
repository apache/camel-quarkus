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
package org.apache.camel.quarkus.variables.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class VariablesTest {

    @Test
    public void testSetLocalVariable() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .post("/variables/setLocalVariable")
                .then()
                .statusCode(200)
                .body(Matchers.is(VariablesRoutes.VARIABLE_VALUE));
    }

    @Test
    public void testSetGlobalVariable() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .post("/variables/setGlobalVariable")
                .then()
                .statusCode(200)
                .body(Matchers.is("null," + VariablesRoutes.VARIABLE_VALUE));
    }

    @Test
    public void testRemoveLocalVariable() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .post("/variables/removeLocalVariable")
                .then()
                .statusCode(200)
                .body(Matchers.is(VariablesRoutes.VARIABLE_VALUE + ",null"));
    }

    @Test
    public void testRemoveGlobalVariable() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .post("/variables/removeGlobalVariable")
                .then()
                .statusCode(200)
                .body(Matchers.is("null,null"));
    }

    @Test
    public void testConvert() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("aaaa")
                .post("/variables/convert")
                .then()
                .statusCode(200)
                .body(Matchers.is("11.0"));
    }

    @Test
    public void testFilter() {
        //first call fails, because city Gotham is filtered Out
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Sheldon")
                .post("/variables/filter/Gotham")
                .then()
                .statusCode(204);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Sheldon")
                .post("/variables/filter/Medford")
                .then()
                .statusCode(200)
                .body(Matchers.is("Sheldon"));
    }
}
