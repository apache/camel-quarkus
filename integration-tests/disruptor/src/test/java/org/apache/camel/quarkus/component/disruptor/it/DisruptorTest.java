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
package org.apache.camel.quarkus.component.disruptor.it;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class DisruptorTest {

    @Test
    public void loadComponent() {
        RestAssured.get("/disruptor/component/{componentName}", DisruptorResource.DISRUPTOR)
                .then()
                .statusCode(200);
        RestAssured.get("/disruptor/component/{componentName}", DisruptorResource.DISRUPTOR_VM)
                .then()
                .statusCode(404);
    }

    @Test
    public void putAndTake() {
        final String id = "the-id";
        final String value = UUID.randomUUID().toString();

        RestAssured.given()
                .body(value)
                .post("/disruptor/buffer/{name}", id)
                .then()
                .statusCode(204);

        await().atMost(10L, TimeUnit.SECONDS).until(
                () -> {
                    int count = RestAssured.given()
                            .accept(MediaType.APPLICATION_JSON)
                            .when()
                            .get("/disruptor/buffer/{name}/inspect", id)
                            .then()
                            .statusCode(200)
                            .extract().path("pendingExchangeCount");

                    return count == 1;
                });

        RestAssured.get("/disruptor/buffer/{name}", id)
                .then()
                .statusCode(200)
                .body(is(value));

        RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .when()
                .get("/disruptor/buffer/{name}/inspect", id)
                .then()
                .statusCode(200)
                .body("pendingExchangeCount", is(0), "size", is(0));
    }
}
