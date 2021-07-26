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
package org.apache.camel.quarkus.component.rss.it;

import java.util.LinkedHashMap;
import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class RssTest {

    //@Test
    public void rssComponentConsumeFeedHttp() {
        JsonPath json = RestAssured.given()
                .queryParam("test-port", RestAssured.port)
                .get("/rss/component")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertFeedContent(json);
    }

    //@Test
    public void rssDataformatMarshalUnmarshal() {
        JsonPath json = RestAssured.get("/rss/dataformat")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertFeedContent(json);
    }

    private void assertFeedContent(JsonPath json) {
        assertEquals("Camel Quarkus Test Feed Title", json.getString("title"));
        assertEquals("Camel Quarkus Test Feed Description", json.getString("description"));
        assertEquals("https://camel.apache.org", json.getString("link"));

        List<LinkedHashMap<String, String>> items = json.getList("items");
        assertEquals(3, items.size());

        for (int i = 0; i < items.size(); i++) {
            LinkedHashMap<String, String> entry = items.get(i);
            int index = i + 1;
            assertEquals("Test item title " + index, entry.get("title"));
            assertEquals("https://camel.apache.org/test-item-" + index, entry.get("link"));
            assertEquals("Test item description " + index, entry.get("description"));
            assertEquals("Apache Camel", entry.get("author"));
        }
    }
}
