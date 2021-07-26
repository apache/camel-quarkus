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
package org.apache.camel.quarkus.component.lra.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource(LraTestResource.class)
class LraTest {

    //@Test
    public void testLraTransaction() {
        // Create valid orders
        RestAssured.given()
                .queryParam("amount", 20)
                .queryParam("fail", false)
                .post("/lra/order")
                .then()
                .statusCode(201);

        RestAssured.given()
                .queryParam("amount", 70)
                .queryParam("fail", false)
                .post("/lra/order")
                .then()
                .statusCode(201);

        RestAssured.given()
                .queryParam("amount", 5)
                .queryParam("fail", false)
                .post("/lra/order")
                .then()
                .statusCode(201);

        // Force failure scenario
        RestAssured.given()
                .queryParam("amount", 20)
                .queryParam("fail", true)
                .post("/lra/order")
                .then()
                .statusCode(500);

        // Verify count of valid orders
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            return RestAssured.get("/lra/order/count")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString().equals("3");
        });

        // Verify credit remaining
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            return RestAssured.get("/lra/credit/available")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString().equals("5");
        });
    }
}
