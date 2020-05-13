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
package org.apache.camel.quarkus.component.mongodb.it;

import java.util.List;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(MongoDbTestResource.class)
class MongoDbTest {
    @Test
    public void testMongoDbComponent() {
        Assumptions.assumeFalse(ConfigProvider.getConfig().getOptionalValue(
                "quarkus.camel.main.lightweight", Boolean.class).orElse(false),
                "TODO: Disabled in lightweight mode");

        // Write to collection
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{message:\"Hello Camel Quarkus Mongo DB\"}")
                .post("/mongodb/collection/camelTest")
                .then()
                .statusCode(201);

        // Retrieve from collection
        JsonPath jsonPath = RestAssured.get("/mongodb/collection/camelTest")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        List<Map<String, String>> documents = jsonPath.get();
        assertEquals(1, documents.size());

        Map<String, String> document = documents.get(0);
        assertEquals("Hello Camel Quarkus Mongo DB", document.get("message"));
    }
}
