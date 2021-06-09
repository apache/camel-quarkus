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
package org.apache.camel.quarkus.component.jackson.avro.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.jackson.avro.it.StringAppendingDeserializer.STRING_TO_APPEND;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class JacksonAvroTest {

    @Test
    public void marshalUnmarshal() {
        String message = "Hello Camel Quarkus Jackson Avro";
        byte[] avroSerialized = RestAssured.given()
                .body(message)
                .post("/jackson-avro/marshal")
                .then()
                .statusCode(200)
                .header("Content-Length", equalTo(String.valueOf(message.length() + 1)))
                .extract()
                .body()
                .asByteArray();

        // Unmarshal to pojo
        RestAssured.given()
                .body(avroSerialized)
                .post("/jackson-avro/unmarshal/pojo")
                .then()
                .statusCode(200)
                .body(equalTo(message));

        // Unmarshal to JsonNode
        RestAssured.given()
                .body(avroSerialized)
                .post("/jackson-avro/unmarshal/json-node")
                .then()
                .statusCode(200)
                .body(equalTo(message));

        // Unmarshal using a pre-defined dataformat bean
        RestAssured.given()
                .body(avroSerialized)
                .post("/jackson-avro/unmarshal/defined-dataformat")
                .then()
                .statusCode(200)
                .body(equalTo(message + STRING_TO_APPEND));
    }

    @Test
    public void marshalUnmarshalWithList() {
        String message = "Hello Camel Quarkus Jackson Avro";

        // Marshal pojo list to avro
        byte[] avroSerialized = RestAssured.given()
                .body(message)
                .post("/jackson-avro/marshal/list")
                .then()
                .statusCode(200)
                .header("Content-Length", equalTo(String.valueOf(message.length() + 3)))
                .extract()
                .body()
                .asByteArray();

        // Unmarshal to pojo list
        RestAssured.given()
                .body(avroSerialized)
                .post("/jackson-avro/unmarshal/pojo-list")
                .then()
                .statusCode(200)
                .body(equalTo(message));
    }
}
