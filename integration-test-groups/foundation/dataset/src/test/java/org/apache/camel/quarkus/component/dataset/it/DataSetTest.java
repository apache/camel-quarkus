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
package org.apache.camel.quarkus.component.dataset.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class DataSetTest {

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void simpleDataSet(boolean useIndexHeader) {
        RestAssured.given()
                .queryParam("useIndexHeader", useIndexHeader)
                .get("/dataset/simple")
                .then()
                .statusCode(204);
    }

    @Test
    public void simpleDataSetException() {
        RestAssured.given()
                .get("/dataset/simple/exception")
                .then()
                .statusCode(200);
    }

    @Test
    public void simpleDataSetConsumer() {
        RestAssured.given()
                .get("/dataset/simple/consumer")
                .then()
                .statusCode(204);
    }

    @Test
    public void listDataSet() {
        RestAssured.given()
                .get("/dataset/list")
                .then()
                .statusCode(204);
    }

    @Test
    public void listDataSetConsumer() {
        RestAssured.given()
                .get("/dataset/list/consumer")
                .then()
                .statusCode(204);
    }

    @Test
    public void fileDataSet() {
        RestAssured.given()
                .get("/dataset/file")
                .then()
                .statusCode(204);
    }

    @Test
    public void fileDataSetDelimited() {
        RestAssured.given()
                .get("/dataset/file/delimited")
                .then()
                .statusCode(204);
    }

    @Test
    public void fileDataSetConsumer() {
        RestAssured.given()
                .get("/dataset/file/consumer")
                .then()
                .statusCode(204);
    }

    @Test
    public void customDataSetProduceConsume() {
        RestAssured.given()
                .get("/dataset/custom")
                .then()
                .statusCode(204);
    }

    @Test
    public void indexOff() {
        RestAssured.given()
                .get("/dataset/simple/index/off")
                .then()
                .statusCode(204);
    }

    @Test
    public void indexLenient() {
        RestAssured.given()
                .get("/dataset/simple/index/lenient")
                .then()
                .statusCode(204);
    }

    @Test
    public void indexStrict() {
        RestAssured.given()
                .queryParam("useIndexHeader", true)
                .get("/dataset/simple/index/strict")
                .then()
                .statusCode(200);
    }

    @Test
    public void indexStrictWithoutIndexHeader() {
        RestAssured.given()
                .queryParam("useIndexHeader", false)
                .get("/dataset/simple/index/strict")
                .then()
                .statusCode(500)
                .body(containsString("No 'CamelDataSetIndex' header available"));
    }

    @Test
    public void preload() {
        RestAssured.given()
                .get("/dataset/preload")
                .then()
                .statusCode(204);
    }

    @Test
    public void datasetTestSeda() {
        RestAssured.given()
                .get("/dataset/test/seda")
                .then()
                .statusCode(204);
    }

    @Test
    public void datasetTestSedaAnyOrder() {
        RestAssured.given()
                .get("/dataset/test/seda/any/order")
                .then()
                .statusCode(204);
    }

    @Test
    public void datasetTestSplit() {
        RestAssured.given()
                .get("/dataset/test/split")
                .then()
                .statusCode(204);
    }
}
