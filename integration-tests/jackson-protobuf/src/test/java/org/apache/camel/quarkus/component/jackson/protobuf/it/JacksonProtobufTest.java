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
package org.apache.camel.quarkus.component.jackson.protobuf.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class JacksonProtobufTest {

    @Test
    public void marshalUnmarshal() {
        String message = "Hello Camel Quarkus Jackson Protobuf";
        byte[] protobufSerialized = RestAssured.given()
                .body(message)
                .post("/jackson-protobuf/marshal")
                .then()
                .statusCode(200)
                .header("Content-Length", equalTo(String.valueOf(message.length() + 2)))
                .extract()
                .body()
                .asByteArray();

        // Unmarshal to pojo
        RestAssured.given()
                .body(protobufSerialized)
                .post("/jackson-protobuf/unmarshal/pojo")
                .then()
                .statusCode(200)
                .body(equalTo(message));

        // Unmarshal to JsonNode
        RestAssured.given()
                .body(protobufSerialized)
                .post("/jackson-protobuf/unmarshal/json-node")
                .then()
                .statusCode(200)
                .body(equalTo(message));

        // Unmarshal using a pre-defined dataformat bean
        RestAssured.given()
                .body(protobufSerialized)
                .post("/jackson-protobuf/unmarshal/defined-dataformat")
                .then()
                .statusCode(200)
                .body(equalTo(message));
    }
}
