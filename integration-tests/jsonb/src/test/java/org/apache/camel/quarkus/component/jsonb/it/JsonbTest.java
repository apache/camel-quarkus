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
package org.apache.camel.quarkus.component.jsonb.it;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class JsonbTest {

    private final Map MSG = new HashMap() {
        {
            put("greeting", "Hello");
            put("name", "Sheldon");
        }
    };
    private final String MARSHALLED_MSG = "\\{\"greeting\":\"Hello\",\"name\":\"Sheldon\"\\}";

    @Test
    public void testMap() throws Exception {
        Map<String, Object> in = new HashMap<>();
        in.put("greeting", "Hello");
        in.put("name", serialize("Sheldon"));

        String marshalled = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(MSG)
                .queryParam("route", "in")
                .post("/jsonb/marshallMap")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertTrue(marshalled.matches(MARSHALLED_MSG),
                String.format("Expected '%s', but got '%s'", MARSHALLED_MSG, marshalled));

        Map unmarshalled = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(marshalled)
                .post("/jsonb/unmarshallMap")
                .then()
                .statusCode(200)
                .extract()
                .as(Map.class);

        assertTrue(unmarshalled.containsKey("greeting"), "Key 'greeting' should be present.");
        assertEquals("Hello", unmarshalled.get("greeting"));
        assertTrue(unmarshalled.containsKey("name"), "Key 'name' should be present.");
        assertEquals("Sheldon", unmarshalled.get("name"));
    }

    @Test
    public void testPojo() throws Exception {
        String s = RestAssured.given()
                .contentType(ContentType.BINARY)
                .body(serialize("Sheldon"))
                .queryParam("route", "in")
                .post("/jsonb/marshallPojo")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertTrue(s.matches("\\{\"name\":\".*\"\\}"), String.format("Expected '{\"name\":\".*\"}', but got '%s'", s));

        InputStream is = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(s)
                .post("/jsonb/unmarshallPojo")
                .then()
                .statusCode(201)
                .extract()
                .asInputStream();

        assertEquals("Sheldon", deserialize(is));
    }

    private byte[] serialize(String s) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(s);
            return baos.toByteArray();
        }
    }

    private Object deserialize(InputStream is) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            return ois.readObject();
        }
    }

}
