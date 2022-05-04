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
package org.apache.camel.quarkus.component.infinispan;

import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(InfinispanServerTestResource.class)
public class InfinispanTest {

    @AfterEach
    public void afterEach() {
        for (String componentName : componentNames()) {
            RestAssured.with()
                    .queryParam("component", componentName)
                    .delete("/infinispan/clear")
                    .then()
                    .statusCode(204);

            RestAssured.with()
                    .queryParam("component", componentName)
                    .get("/infinispan/get")
                    .then()
                    .statusCode(204);
        }
    }

    @Disabled("https://github.com/apache/camel-quarkus/issues/3657")
    @ParameterizedTest
    @MethodSource("componentNames")
    public void aggregate(String componentName) {
        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/aggregate")
                .then()
                .statusCode(204);
    }

    @ParameterizedTest
    @MethodSource("componentNamesWithSynchronicity")
    public void clear(String componentName, boolean isAsync) {
        RestAssured.with()
                .queryParam("component", componentName)
                .body("Hello " + componentName)
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Hello " + componentName));

        RestAssured.with()
                .queryParam("component", componentName)
                .delete(computePath("/infinispan/clear", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/get")
                .then()
                .statusCode(204);
    }

    @ParameterizedTest
    @MethodSource("componentNamesWithSynchronicity")
    public void compute(String componentName, boolean isAsync) {
        RestAssured.with()
                .queryParam("component", componentName)
                .body("Initial value")
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .post(computePath("/infinispan/compute", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Initial value-remapped"));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void containsKey(String componentName) {
        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/containsKey")
                .then()
                .statusCode(200)
                .body(is("false"));

        RestAssured.with()
                .queryParam("component", componentName)
                .body("Hello " + componentName)
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/containsKey")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void containsValue(String componentName) {
        String value = "test-value";

        RestAssured.with()
                .queryParam("component", componentName)
                .queryParam("value", value)
                .get("/infinispan/containsValue")
                .then()
                .statusCode(200)
                .body(is("false"));

        RestAssured.with()
                .queryParam("component", componentName)
                .body(value)
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .queryParam("value", value)
                .get("/infinispan/containsValue")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void customListener(String componentName) {
        RestAssured.with()
                .queryParam("component", componentName)
                .post("/infinispan/consumer/" + componentName + "-custom-listener/true")
                .then()
                .statusCode(204);

        try {
            String mockEndpointUri = componentName.equals("infinispan") ? "mock:camelResultCustomListener"
                    : "mock:quarkusResultCustomListener";
            RestAssured.with()
                    .queryParam("component", componentName)
                    .queryParam("mockEndpointUri", mockEndpointUri)
                    .body("Hello " + componentName)
                    .get("/infinispan/event/verify")
                    .then()
                    .statusCode(204);
        } finally {
            RestAssured.with()
                    .queryParam("component", componentName)
                    .post("/infinispan/consumer/" + componentName + "-custom-listener/false")
                    .then()
                    .statusCode(204);
        }
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void events(String componentName) {
        RestAssured.with()
                .queryParam("component", componentName)
                .post("/infinispan/consumer/" + componentName + "-events/true")
                .then()
                .statusCode(204);

        try {
            String mockEndpointUri = componentName.equals("infinispan") ? "mock:camelResultCreated"
                    : "mock:quarkusResultCreated";
            RestAssured.with()
                    .queryParam("component", componentName)
                    .queryParam("mockEndpointUri", mockEndpointUri)
                    .body("Hello " + componentName)
                    .get("/infinispan/event/verify")
                    .then()
                    .statusCode(204);
        } finally {
            RestAssured.with()
                    .queryParam("component", componentName)
                    .post("/infinispan/consumer/" + componentName + "-events/false")
                    .then()
                    .statusCode(204);
        }
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void getOrDefault(String componentName) {
        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/getOrDefault")
                .then()
                .statusCode(200)
                .body(is("default-value"));

        RestAssured.with()
                .queryParam("component", componentName)
                .body("Hello " + componentName)
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/getOrDefault")
                .then()
                .statusCode(200)
                .body(is("Hello " + componentName));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void idempotent(String componentName) {
        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/putIdempotent")
                .then()
                .statusCode(204);
    }

    @Test
    public void inspect() {
        RestAssured.when()
                .get("/infinispan/inspect")
                .then().body(
                        "hosts", is(notNullValue()),
                        "cache-manager", is("none"));
    }

    @ParameterizedTest
    @MethodSource("componentNamesWithSynchronicity")
    public void put(String componentName, boolean isAsync) {
        RestAssured.with()
                .queryParam("component", componentName)
                .body("Hello " + componentName)
                .post(computePath("/infinispan/put", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Hello " + componentName));
    }

    @ParameterizedTest
    @MethodSource("componentNamesWithSynchronicity")
    public void putAll(String componentName, boolean isAsync) {
        RestAssured.with()
                .queryParam("component", componentName)
                .post(computePath("/infinispan/putAll", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .queryParam("key", "key-1")
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("value-1"));

        RestAssured.with()
                .queryParam("component", componentName)
                .queryParam("key", "key-2")
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("value-2"));
    }

    @ParameterizedTest
    @MethodSource("componentNamesWithSynchronicity")
    public void putIfAbsent(String componentName, boolean isAsync) {
        RestAssured.with()
                .queryParam("component", componentName)
                .body("Hello " + componentName)
                .post(computePath("/infinispan/putIfAbsent", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Hello " + componentName));

        RestAssured.with()
                .queryParam("component", componentName)
                .body("An alternative value")
                .post(computePath("/infinispan/putIfAbsent", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Hello " + componentName));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void query(String componentName) {
        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/query")
                .then()
                .statusCode(200);
    }

    @ParameterizedTest
    @MethodSource("componentNamesWithSynchronicity")
    public void remove(String componentName, boolean isAsync) {
        RestAssured.with()
                .queryParam("component", componentName)
                .body("Hello " + componentName)
                .post(computePath("/infinispan/put", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .delete(computePath("/infinispan/remove", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/get")
                .then()
                .statusCode(204);
    }

    @ParameterizedTest
    @MethodSource("componentNamesWithSynchronicity")
    public void replace(String componentName, boolean isAsync) {
        RestAssured.with()
                .queryParam("component", componentName)
                .body("Hello " + componentName)
                .post(computePath("/infinispan/put", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("Hello " + componentName));

        RestAssured.with()
                .queryParam("component", componentName)
                .body("replaced cache value")
                .patch(computePath("/infinispan/replace", isAsync))
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/get")
                .then()
                .statusCode(200)
                .body(is("replaced cache value"));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void size(String componentName) {
        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/size")
                .then()
                .statusCode(200)
                .body(is("0"));

        RestAssured.with()
                .queryParam("component", componentName)
                .body("Hello " + componentName)
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/size")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void stats(String componentName) {
        RestAssured.with()
                .queryParam("component", componentName)
                .get("/infinispan/stats")
                .then()
                .statusCode(200)
                .body(is("0"));

        RestAssured.with()
                .queryParam("component", componentName)
                .body("Hello " + componentName)
                .post("/infinispan/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
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

    public static String[] componentNames() {
        return new String[] {
                "infinispan",
                "infinispan-quarkus"
        };
    }

    public static Stream<Arguments> componentNamesWithSynchronicity() {
        return Stream.of(
                Arguments.of("infinispan", false),
                Arguments.of("infinispan-quarkus", false),
                Arguments.of("infinispan", true),
                Arguments.of("infinispan-quarkus", true));
    }
}
