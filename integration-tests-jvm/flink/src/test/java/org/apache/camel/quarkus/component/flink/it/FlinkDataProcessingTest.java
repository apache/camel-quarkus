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
package org.apache.camel.quarkus.component.flink.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class FlinkDataProcessingTest {

    private static final String LINES = "foo\nbar\nbaz\nqux\nquux";

    @Test
    void dataSetCallbackWithSinglePayload() {
        given().contentType(ContentType.TEXT).body(LINES).queryParam("factor", 10)
                .post("/flink/processing/dataset/callback/payload")
                .then().statusCode(200).body(is("50"));
    }

    @Test
    void dataSetCallbackWithMultiplePayloads() {
        given().contentType(ContentType.TEXT).body(LINES).queryParam("factor", 10, 20)
                .post("/flink/processing/dataset/callback/payload")
                .then().statusCode(200).body(is("1000"));
    }

    @Test
    void annotatedDataSetCallback() {
        given().contentType(ContentType.TEXT).body(LINES)
                .post("/flink/processing/dataset/callback/annotated")
                .then().statusCode(200).body(is("5"));
    }

    @Test
    void annotatedDataSetCallbackWithParameters() {
        given().contentType(ContentType.TEXT).body(LINES).queryParam("factor", 10, 20)
                .post("/flink/processing/dataset/callback/annotated")
                .then().statusCode(200).body(is("1000"));
    }

    @Test
    void collectDataSetWithPojoSerialization() {
        given().contentType(ContentType.TEXT).body("camel\nquarkus\ncamel")
                .post("/flink/processing/dataset/collect")
                .then().statusCode(200).body(is("camel:2,quarkus:1"));
    }

    @Test
    void voidDataSetCallback() {
        given().contentType(ContentType.TEXT).body(LINES)
                .post("/flink/processing/dataset/sink/void")
                .then().statusCode(200).body(is(LINES + "\n"));
    }

    @Test
    void annotatedVoidDataSetCallback() {
        given().contentType(ContentType.TEXT).body(LINES)
                .post("/flink/processing/dataset/sink/annotated")
                .then().statusCode(200).body(is(LINES + "\n"));
    }

    @Test
    void collectDataStreamWithTupleSerialization() {
        given().contentType(ContentType.TEXT).body("camel\n\nquarkus")
                .post("/flink/processing/datastream/collect")
                .then().statusCode(200).body(is("CAMEL:5,QUARKUS:7"));
    }

}
