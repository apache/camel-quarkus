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
package org.apache.camel.quarkus.component.kamelet.it;

import java.util.ArrayList;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class KameletTest {

    @Test
    public void testKameletProducing() {
        String message = "Camel Quarkus Kamelet";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/kamelet/produce")
                .then()
                .statusCode(200)
                .body(is("Hello " + message));
    }

    @Test
    public void testKameletConsuming() {
        RestAssured.get("/kamelet/consume")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    public void testKameletWithProperties() {
        RestAssured.get("/kamelet/property")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Kamelet Property"));
    }

    @Test
    public void testKameletChain() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Kamelet")
                .post("/kamelet/chain")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Kamelet Chained Route"));
    }

    @Test
    public void testInvoke() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Kamelet")
                .post("/kamelet/invoke/AppendWithBean")
                .then()
                .statusCode(200)
                .body(is("Kamelet-suffix"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Kamelet2")
                .post("/kamelet/invoke/AppendWithClass")
                .then()
                .statusCode(200)
                .body(is("Kamelet2-suffix"));
    }

    @Test
    public void testDiscovered() {
        Response resp = RestAssured.given()
                .contentType(ContentType.JSON)
                .when().get("/kamelet/list");
        resp.then().statusCode(200);

        ArrayList<Map<String, ?>> jsonAsArrayList = resp.body()
                .jsonPath().get("");
        assertTrue(jsonAsArrayList.contains("injector"));
        assertTrue(jsonAsArrayList.contains("logger"));
    }
}
