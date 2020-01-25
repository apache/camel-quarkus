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
package org.apache.camel.quarkus.component.jackson;

import java.util.UUID;

import javax.json.bind.JsonbBuilder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.jackson.model.DummyObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class JacksonTest {
    @Test
    public void testRoutes() {
        RestAssured.given().contentType(ContentType.TEXT)
                .body("[{\"dummy\": \"value1\"}, {\"dummy\": \"value2\"}]")
                .post("/jackson/in");
        RestAssured.post("/jackson/out")
                .then()
                .body(equalTo("{\"dummy\":\"value1\"}"));
        RestAssured.post("/jackson/out")
                .then()
                .body(equalTo("{\"dummy\":\"value2\"}"));
    }

    @Test
    public void testUnmarshallingDifferentPojos() {
        String bodyA = "{\"name\":\"name A\"}";
        String bodyB = "{\"value\":1.0}";

        RestAssured.given().contentType(ContentType.TEXT)
                .body(bodyA)
                .post("/jackson/in-a");
        RestAssured.given().contentType(ContentType.TEXT)
                .body(bodyB)
                .post("/jackson/in-b");
        RestAssured.post("/jackson/out-a")
                .then()
                .body(equalTo(bodyA));
        RestAssured.post("/jackson/out-b")
                .then()
                .body(equalTo(bodyB));
    }

    @ParameterizedTest
    @ValueSource(strings = { "type-as-attribute", "type-as-header" })
    public void testUnmarshal(String directId) {
        DummyObject object = new DummyObject();
        object.setDummy(UUID.randomUUID().toString());

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(JsonbBuilder.create().toJson(object))
                .post("/jackson/unmarshal/{direct-id}", directId)
                .then()
                .body("dummy", is(object.getDummy()));
    }
}
