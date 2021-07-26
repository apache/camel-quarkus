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
package org.apache.camel.quarkus.component.atom.it;

import java.util.LinkedHashMap;
import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class AtomTest {

    //@Test
    public void consumeAtomFeed() {
        JsonPath json = RestAssured.given()
                .queryParam("test-port", RestAssured.port)
                .get("/atom/feed")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertEquals("Camel Quarkus Test Feed Title", json.getString("title"));
        assertEquals("Camel Quarkus Test Feed Subtitle", json.getString("subtitle"));
        assertEquals("https://camel.apache.org", json.getString("link"));

        List<LinkedHashMap<String, String>> entries = json.getList("entries");
        assertEquals(3, entries.size());

        for (int i = 0; i < entries.size(); i++) {
            LinkedHashMap<String, String> entry = entries.get(i);
            int index = i + 1;
            assertEquals("Test entry title " + index, entry.get("title"));
            assertEquals("https://camel.apache.org/test-entry-" + index, entry.get("link"));
            assertEquals("Test entry summary " + index, entry.get("summary"));
            assertEquals("Test entry content " + index, entry.get("content"));
            assertEquals("Apache Camel", entry.get("author"));
        }
    }
}
