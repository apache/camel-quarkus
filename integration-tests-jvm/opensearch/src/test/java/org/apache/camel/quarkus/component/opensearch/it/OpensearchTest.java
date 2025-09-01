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
package org.apache.camel.quarkus.component.opensearch.it;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(OpensearchTestResource.class)
class OpensearchTest {

    @ParameterizedTest
    @MethodSource("docProvider")
    void testIndexGetDeleteScroll(DocData doc) {
        // ✅ Index
        given()
                .contentType("application/json")
                .body(doc.json)
                .post("/opensearch/index/{index}/{id}", doc.indexName, doc.id)
                .then()
                .statusCode(200)
                .and()
                .body(is(doc.id));

        // ✅ Get
        given()
                .get("/opensearch/get/{index}/{id}", doc.indexName, doc.id)
                .then()
                .statusCode(200)
                .body(containsString(doc.id));

        // ✅ Search with scroll
        String scrollResponse = given()
                .contentType("application/json")
                .body("{\"query\":{\"match_all\":{}},\"size\":1}")
                .post("/opensearch/search/{index}", doc.indexName)
                .then()
                .statusCode(200)
                .extract().asString();

        String scrollId = extractScrollId(scrollResponse);
        if (scrollId != null) {
            given()
                    .contentType("application/json")
                    .body("{\"scroll\":\"1m\",\"scroll_id\":\"" + scrollId + "\"}")
                    .post("/opensearch/scroll")
                    .then()
                    .statusCode(200);
        }

        // ✅ Delete
        given()
                .delete("/opensearch/delete/{index}/{id}", doc.indexName, doc.id)
                .then()
                .statusCode(200);
    }

    @Test
    void testBulkIndexMultiGetMultiSearch() {

        // Bulk Insert
        given()
                .contentType("application/json")
                .body(buildBulkDoc())
                .post("/opensearch/bulk/users")
                .then()
                .statusCode(200)
                .body("$", hasSize(4));

        // Verify bulk doc using multiGet
        List<String> ids = Arrays.asList("u1", "u2");

        given()
                .contentType("application/json")
                .body(ids)
                .post("/opensearch/multiget/users")
                .then()
                .statusCode(200)
                .body(is("2"));

        // Multi Search

        String totalFound = given()
                .contentType("application/json")
                .get("/opensearch/multisearch?users=Alice&orders=Phone")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        totalFound.equals("2");

    }

    static Stream<DocData> docProvider() {
        return Stream.of(
                new DocData("users", "u1", "{\"id\":\"u1\",\"name\":\"Alice\"}"),
                new DocData("users", "u2", "{\"id\":\"u2\",\"name\":\"Bob\"}"),
                new DocData("orders", "o1", "{\"id\":\"o1\",\"item\":\"Laptop\"}"),
                new DocData("orders", "o2", "{\"id\":\"o2\",\"item\":\"Phone\"}"));
    }

    static List<Map<String, Object>> buildBulkDoc() {
        return docProvider().map(s -> {
            try {
                return Map.of("index", s.indexName,
                        "id", s.id, "source", new ObjectMapper().readValue(s.json, Map.class));
            } catch (Exception e) {

            }
            return null;
        })
                .collect(Collectors.toList());
    }

    private String extractScrollId(String response) {
        if (response.contains("\"_scroll_id\"")) {
            int start = response.indexOf("\"_scroll_id\"") + 13;
            int end = response.indexOf("\"", start + 1);
            return response.substring(start + 1, end);
        }
        return null;
    }

    static class DocData {
        String indexName;
        String id;
        String json;

        DocData(String indexName, String id, String json) {
            this.indexName = indexName;
            this.id = id;
            this.json = json;
        }
    }

}
