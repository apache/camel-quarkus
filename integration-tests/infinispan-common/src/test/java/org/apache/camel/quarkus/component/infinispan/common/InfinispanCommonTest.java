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
package org.apache.camel.quarkus.component.infinispan.common;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.is;

public abstract class InfinispanCommonTest {

    @AfterEach
    public void afterEach() {
        RestAssured.with()
                .delete("/infinispan/clear")
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/get")
                .then()
                .statusCode(204);
    }

    @Test
    public void aggregate() {
        RestAssured.with()
                .queryParam("component")
                .get("/infinispan/aggregate")
                .then()
                .statusCode(204);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void clear(boolean isAsync) {
        RestAssured.with()
                .body("Hello Camel Infinispan")
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Infinispan"));

        RestAssured.with()
                .delete(computePath("/infinispan/clear", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/get")
                .then()
                .statusCode(204);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void compute(boolean isAsync) {
        RestAssured.with()
                .body("Initial value")
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .post(computePath("/infinispan/compute", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Initial value-remapped"));
    }

    @Test
    public void containsKey() {
        RestAssured.with()
                .get("/infinispan/containsKey")
                .then()
                .statusCode(200)
                .body(is("false"));

        RestAssured.with()
                .body("Hello Camel Infinispan")
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/containsKey")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void containsValue() {
        String value = "test-value";

        RestAssured.with()
                .queryParam("value", value)
                .get("/infinispan/containsValue")
                .then()
                .statusCode(200)
                .body(is("false"));

        RestAssured.with()
                .body(value)
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("value", value)
                .get("/infinispan/containsValue")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void customListener() {
        RestAssured.with()
                .post("/infinispan/consumer/infinispan-custom-listener/true")
                .then()
                .statusCode(204);

        try {
            RestAssured.with()
                    .queryParam("mockEndpointUri", "mock:resultCustomListener")
                    .body("Hello Camel Infinispan")
                    .get("/infinispan/event/verify")
                    .then()
                    .statusCode(204);
        } finally {
            RestAssured.with()
                    .post("/infinispan/consumer/infinispan-custom-listener/false")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    public void events() {
        RestAssured.with()
                .post("/infinispan/consumer/infinispan-events/true")
                .then()
                .statusCode(204);

        try {
            RestAssured.with()
                    .queryParam("mockEndpointUri", "mock:resultCreated")
                    .body("Hello Camel Infinispan")
                    .get("/infinispan/event/verify")
                    .then()
                    .statusCode(204);
        } finally {
            RestAssured.with()
                    .post("/infinispan/consumer/infinispan-events/false")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    public void getOrDefault() {
        RestAssured.with()
                .get("/infinispan/getOrDefault")
                .then()
                .statusCode(200)
                .body(is("default-value"));

        RestAssured.with()
                .body("Hello Camel Infinispan")
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/getOrDefault")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Infinispan"));
    }

    @Test
    public void idempotent() {
        RestAssured.with()
                .get("/infinispan/putIdempotent")
                .then()
                .statusCode(204);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void put(boolean isAsync) {
        RestAssured.with()
                .body("Hello Camel Infinispan")
                .post(computePath("/infinispan/put", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Infinispan"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void putAll(boolean isAsync) {
        RestAssured.with()
                .post(computePath("/infinispan/putAll", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("key", "key-1")
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("value-1"));

        RestAssured.with()
                .queryParam("key", "key-2")
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("value-2"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void putIfAbsent(boolean isAsync) {
        RestAssured.with()
                .body("Hello Camel Infinispan")
                .post(computePath("/infinispan/putIfAbsent", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Infinispan"));

        RestAssured.with()
                .body("An alternative value")
                .post(computePath("/infinispan/putIfAbsent", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Infinispan"));
    }

    @Test
    public void query() {
        RestAssured.with()
                .get("/infinispan/query")
                .then()
                .statusCode(200);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void remove(boolean isAsync) {
        RestAssured.with()
                .body("Hello Camel Infinispan")
                .post(computePath("/infinispan/put", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .delete(computePath("/infinispan/remove", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()

                .get("/infinispan/get")
                .then()
                .statusCode(204);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void replace(boolean isAsync) {
        RestAssured.with()
                .body("Hello Camel Infinispan")
                .post(computePath("/infinispan/put", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Infinispan"));

        RestAssured.with()
                .body("replaced cache value")
                .patch(computePath("/infinispan/replace", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("replaced cache value"));
    }

    @Test
    public void size() {
        RestAssured.with()
                .get("/infinispan/size")
                .then()
                .statusCode(200)
                .body(is("0"));

        RestAssured.with()
                .body("Hello Camel Infinispan")
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/size")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    public void stats() {
        RestAssured.with()
                .get("/infinispan/stats")
                .then()
                .statusCode(200)
                .body(is("0"));

        RestAssured.with()
                .body("Hello Camel Infinispan")
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .get("/infinispan/stats")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    private String computePath(String path, boolean isAsync) {
        if (isAsync) {
            path += "Async";
        }
        return path;
    }
}
