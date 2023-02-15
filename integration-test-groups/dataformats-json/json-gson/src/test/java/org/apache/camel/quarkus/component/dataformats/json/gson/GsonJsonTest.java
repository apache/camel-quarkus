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
package org.apache.camel.quarkus.component.dataformats.json.gson;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.json.bind.JsonbBuilder;
import org.apache.camel.quarkus.component.dataformats.json.gson.model.AnotherObject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@QuarkusTest
public class GsonJsonTest {

    @Test
    public void testMarshallAndUnmarshall() {
        RestAssured.given().contentType(ContentType.TEXT)
                .body("[{\"dummy_string\": \"value1\"}, {\"dummy_string\": \"value2\"}]")
                .post("/dataformats-json-gson/in");
        RestAssured.given()
                .post("/dataformats-json-gson/out")
                .then()
                .body("dummy_string", equalTo("value1"))
                .body("date", containsString("1970"));
        RestAssured.given()
                .post("/dataformats-json-gson/out")
                .then()
                .body("dummy_string", equalTo("value2"))
                .body("date", containsString("1970"));
    }

    @Test
    public void testUnmarshallingDifferentPojos() {
        String bodyA = "{\"name\":\"name A\"}";
        String bodyB = "{\"value\":1.0}";

        RestAssured.given().contentType(ContentType.TEXT)
                .body(bodyA)
                .post("/dataformats-json-gson/in-a");
        RestAssured.given().contentType(ContentType.TEXT)
                .body(bodyB)
                .post("/dataformats-json-gson/in-b");
        RestAssured.given()
                .post("/dataformats-json-gson/out-a")
                .then()
                .body(equalTo(bodyA));
        RestAssured.given()
                .post("/dataformats-json-gson/out-b")
                .then()
                .body(equalTo(bodyB));
    }

    @Test
    public void testUnmarshallTypeAsAttribute() {
        testUnmarshal("Gson-type-as-attribute");
    }

    @Test
    public void testUnmarshallTypeAsHeader() {
        testUnmarshal("Gson-type-as-header");
    }

    public void testUnmarshal(String directId) {
        AnotherObject object = new AnotherObject();
        object.setDummyString("95f669ce-d287-4519-b212-4450bc791867");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(JsonbBuilder.create().toJson(object))
                .post("/dataformats-json-gson/unmarshal/{direct-id}", directId)
                .then()
                .body("dummyString", is(object.getDummyString()));
    }

}
