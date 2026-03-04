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
package org.apache.camel.quarkus.component.milvus.it;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.containsStringIgnoringCase;

@QuarkusTest
@QuarkusTestResource(MilvusTestResource.class)
class MilvusTest {

    @ParameterizedTest
    @MethodSource("provideScenarios")
    public void testMilvusOperations(String operation, Map<String, Object> config, Object data,
            int status,
            String error) {
        String collectionName = (String) config.get("name");
        var response = given()
                .contentType(ContentType.JSON)
                .body(data == null ? config : data)
                .post("/milvus/" + operation + "/" + collectionName);

        response.then().statusCode(status);

        if (error != null) {
            response.then().body(containsStringIgnoringCase(error));
        }
    }

    private static Stream<Arguments> provideScenarios() {

        Map<String, Object> config3d = Map.of(
                "name", "col_3d",
                "dimension", 3,
                "vector_field", "vector",
                "autoID", false);

        Map<String, Object> ghostConfig = Map.of("name", "non_existent_collection");

        return Stream.of(
                // 1. Create Collection
                Arguments.of("create", config3d, null, 200, "Success"),

                // 2. Insert Data
                Arguments.of("insert", config3d,
                        List.of(Map.of("id", 1L, "vector", List.of(0.1f, 0.2f, 0.3f))), 200, null),
                Arguments.of("insert", config3d,
                        List.of(Map.of("id", 2L, "vector", List.of(0.4f, 0.6f, 0.8f))), 200, null),
                // 3. Index Data
                Arguments.of("index", config3d, Map.of(
                        "vector_field", "vector", "params", "{\"nlist\": 1024}"), 200, "Success"),
                // 4. Search
                Arguments.of("search", config3d, List.of(0.1f, 0.2f, 0.3f), 200, null),

                // 5. Delete
                Arguments.of("delete", config3d, List.of(1L), 200, "delete_cnt: 1"),

                // Negative Use Case

                // 1. Invalid insert
                Arguments.of("insert", config3d,
                        List.of(Map.of("id", 4L, "vector", List.of(0.1f, 0.2f))), 500, "Milvus Exception"),
                // 2. Invalid Search
                Arguments.of("search", config3d, List.of(0.1f, 0.2f), 400, "dimension mismatch"),

                Arguments.of("search", ghostConfig, List.of(0.1f, 0.2f), 400, "collection not found"),
                // 3. Invalid delete
                Arguments.of("delete", ghostConfig, List.of(1L), 400, "collection not found"));

    }

}
