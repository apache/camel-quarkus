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
package org.apache.camel.quarkus.component.protobuf.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.protobuf.it.model.AddressBookProtos.Person;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class ProtobufTest {
    final static int id = 2345;
    final static String name = "Joe";
    final static String json = "{\"name\": \"" + name + "\",\"id\": " + id + "}";
    final static Person person = Person.newBuilder()
            .setId(id)
            .setName(name)
            .build();
    final static byte[] protobuf = person.toByteArray();

    @Test
    void marshal() {

        final byte[] actual = RestAssured.given()
                .contentType("application/json")
                .queryParam("name", name)
                .queryParam("id", id)
                .get("/protobuf/marshal")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asByteArray();

        Assertions.assertArrayEquals(protobuf, actual);
    }

    @Test
    void unmarshal() {

        RestAssured.given()
                .contentType("application/octet-stream")
                .body(protobuf)
                .post("/protobuf/unmarshal")
                .then()
                .statusCode(200)
                .body(equalTo(json));

    }

    @Test
    void marshalJson() {

        RestAssured.given()
                .queryParam("name", name)
                .queryParam("id", id)
                .get("/protobuf/marshal-json")
                .then()
                .statusCode(200)
                .body("name", equalTo(name))
                .body("id", equalTo(id));

    }

    @Test
    void unmarshalJson() {

        RestAssured.given()
                .body(json)
                .post("/protobuf/unmarshal-json")
                .then()
                .statusCode(200)
                .body(is("Joe - 2345"));

    }
}
