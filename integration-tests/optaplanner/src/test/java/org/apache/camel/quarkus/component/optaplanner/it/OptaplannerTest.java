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
package org.apache.camel.quarkus.component.optaplanner.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;

@Disabled("https://github.com/apache/camel-quarkus/issues/4116")
@QuarkusTest
class OptaplannerTest {

    @Test
    public void solveSync() {
        // Initiate synchronous solving
        RestAssured.given()
                .post("/optaplanner/solveSync")
                .then()
                .statusCode(204);

        // Poll for results
        await().pollDelay(250, TimeUnit.MILLISECONDS).atMost(1, TimeUnit.MINUTES).until(() -> {
            JsonPath json = RestAssured.given()
                    .get("/optaplanner/solution/solveSync")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .jsonPath();

            return json.getLong("timeslot") > -1 && json.getLong("room") > -1;
        });
    }

    @Test
    public void solveAsync() {
        // Initiate asynchronous solving
        RestAssured.given()
                .post("/optaplanner/solveAsync")
                .then()
                .statusCode(204);

        // Poll for results
        await().pollDelay(250, TimeUnit.MILLISECONDS).atMost(1, TimeUnit.MINUTES).until(() -> {
            JsonPath json = RestAssured.given()
                    .get("/optaplanner/solution/solveAsync")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .jsonPath();

            return json.getLong("timeslot") > -1 && json.getLong("room") > -1;
        });
    }

    @Test
    public void optaplannerConsumerBestSolution() {
        try {
            // Start optaplanner consumer
            RestAssured.given()
                    .post("/optaplanner/consumer/true")
                    .then()
                    .statusCode(204);

            // Initiate asynchronous solving
            RestAssured.given()
                    .post("/optaplanner/solveAsync")
                    .then()
                    .statusCode(204);

            // Poll for results
            await().pollDelay(250, TimeUnit.MILLISECONDS).atMost(1, TimeUnit.MINUTES).until(() -> {
                JsonPath json = RestAssured.given()
                        .get("/optaplanner/solution/solveAsync")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .jsonPath();

                return json.getLong("timeslot") > -1 && json.getLong("room") > -1;
            });

            await().pollDelay(250, TimeUnit.MILLISECONDS).atMost(1, TimeUnit.MINUTES).until(() -> {
                JsonPath json = RestAssured.given()
                        .get("/optaplanner/solution/bestSolution")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .jsonPath();

                return json.getLong("timeslot") > -1 && json.getLong("room") > -1;
            });
        } finally {
            // Stop optaplanner consumer
            RestAssured.given()
                    .post("/optaplanner/consumer/false")
                    .then()
                    .statusCode(204);
        }
    }

}
