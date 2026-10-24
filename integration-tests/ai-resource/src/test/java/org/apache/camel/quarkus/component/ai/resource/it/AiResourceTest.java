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
package org.apache.camel.quarkus.component.ai.resource.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AiResourceTest {

    @Test
    void resourceRegistrationByTag() {
        RestAssured.get("/ai-resource/resources/tag/config")
                .then()
                .statusCode(200)
                .body(containsString("appConfig"));

        RestAssured.get("/ai-resource/resources/tag/reports")
                .then()
                .statusCode(200)
                .body(containsString("latestReport"));
    }

    @Test
    void taglessResourceInDefaultPool() {
        RestAssured.get("/ai-resource/resources/all")
                .then()
                .statusCode(200)
                .body(containsString("readme"));
    }

    @Test
    void defaultPoolMergedWithTaggedResources() {
        RestAssured.get("/ai-resource/resources/tag/config")
                .then()
                .statusCode(200)
                .body(containsString("readme"));
    }

    @Test
    void allResourcesReturnsEveryRegisteredResource() {
        String resources = RestAssured.get("/ai-resource/resources/all")
                .then()
                .statusCode(200)
                .extract().asString();

        assertTrue(resources.contains("appConfig"), "Expected appConfig in: " + resources);
        assertTrue(resources.contains("latestReport"), "Expected latestReport in: " + resources);
        assertTrue(resources.contains("readme"), "Expected readme in: " + resources);
        assertTrue(resources.contains("failingResource"), "Expected failingResource in: " + resources);
    }

    @Test
    void resourceDescription() {
        RestAssured.get("/ai-resource/resources/appConfig/description")
                .then()
                .statusCode(200)
                .body(is("Current application configuration"));
    }

    @Test
    void readTextResource() {
        RestAssured.get("/ai-resource/read/appConfig")
                .then()
                .statusCode(200)
                .body(is("{\"env\":\"test\"}"));
    }

    @Test
    void readBinaryResource() {
        byte[] content = RestAssured.get("/ai-resource/read/latestReport")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        assertArrayEquals(new byte[] { 0x25, 0x50, 0x44, 0x46 }, content);
    }

    @Test
    void readFileResourceOnEveryRead() {
        writeNotes("first version");
        RestAssured.get("/ai-resource/read/notes")
                .then()
                .statusCode(200)
                .body(is("first version"));

        // the route reads the file again, nothing is cached
        writeNotes("second version");
        RestAssured.get("/ai-resource/read/notes")
                .then()
                .statusCode(200)
                .body(is("second version"));
    }

    @Test
    void readJsonResourceMarshalledByJackson() {
        RestAssured.get("/ai-resource/read/latestOrder")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", is("A-1"), "quantity", is(2));
    }

    @Test
    void readFailingResource() {
        RestAssured.get("/ai-resource/read/failingResource")
                .then()
                .statusCode(500);
    }

    @Test
    void readNonExistentResource() {
        RestAssured.get("/ai-resource/read/nonExistent")
                .then()
                .statusCode(404);
    }

    private static void writeNotes(String content) {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(content)
                .put("/ai-resource/notes")
                .then()
                .statusCode(204);
    }
}
