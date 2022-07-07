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
package org.apache.camel.quarkus.component.jq.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JqTest {

    @Test
    public void filter() {
        RestAssured.given()
                .get("/jq/filter")
                .then()
                .statusCode(204);
    }

    @Test
    public void expression() {
        RestAssured.given()
                .get("/jq/expression")
                .then()
                .statusCode(204);
    }

    @Test
    public void expressionHeader() {
        RestAssured.given()
                .get("/jq/expression/header")
                .then()
                .statusCode(204);
    }

    @Test
    public void expressionHeaderFunction() {
        RestAssured.given()
                .get("/jq/expression/header/function")
                .then()
                .statusCode(204);
    }

    @Test
    public void expressionHeaderString() {
        RestAssured.given()
                .get("/jq/expression/header/string")
                .then()
                .statusCode(204);
    }

    @Test
    public void expressionPojo() {
        RestAssured.given()
                .get("/jq/expression/pojo")
                .then()
                .statusCode(204);
    }

    @Test
    public void expressionProperty() {
        RestAssured.given()
                .get("/jq/expression/property")
                .then()
                .statusCode(204);
    }

    @Test
    public void expressionPropertyFunction() {
        RestAssured.given()
                .get("/jq/expression/property/function")
                .then()
                .statusCode(204);
    }
}
