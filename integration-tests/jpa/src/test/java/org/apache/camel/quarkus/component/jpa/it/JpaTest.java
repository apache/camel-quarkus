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
package org.apache.camel.quarkus.component.jpa.it;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.json.bind.JsonbBuilder;
import org.apache.camel.quarkus.component.jpa.it.model.Fruit;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class JpaTest {

    static final String[] FRUITS = new String[] { "Orange", "Lemon", "Plum" };

    @BeforeAll
    public static void storeFruits() {
        final Config config = ConfigProvider.getConfig();
        int port = config.getValue("quarkus.http.test-port", int.class);
        RestAssured.port = port;
        for (String fruit : FRUITS) {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(JsonbBuilder.create().toJson(new Fruit(fruit)))
                    .post("/jpa/fruit")
                    .then()
                    .statusCode(201);
        }
    }

    @Test
    public void testProducerQuery() {
        RestAssured.get("/jpa/fruit")
                .then()
                .statusCode(200)
                .body("name", containsInAnyOrder(FRUITS));
    }

    @Test
    public void testProducerNamedQuery() {
        RestAssured.get("/jpa/fruit/named/" + FRUITS[0])
                .then()
                .statusCode(200)
                .body("name", contains(FRUITS[0]));
    }

    @Test
    public void testProducerNativeQuery() {
        RestAssured.get("/jpa/fruit/native/2")
                .then()
                .statusCode(200)
                .body("name", contains(FRUITS[1]));
    }

    @Test
    public void testConsumer() {
        IntStream.range(1, 3).parallel().forEach((id) -> {
            await().atMost(10L, TimeUnit.SECONDS).until(() -> findFruit(id).getProcessed());
        });

        RestAssured.get("/jpa/mock/processed")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(3));
    }

    @Test
    public void testTransaction() {
        final Fruit rejected = new Fruit("Potato");

        final int acceptedId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(JsonbBuilder.create().toJson(new Fruit("Grapes")))
                .post("/jpa/direct/transaction")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", is("Grapes"))
                .extract().jsonPath().getInt("id");

        try {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(JsonbBuilder.create().toJson(rejected))
                    .header("rollback", true)
                    .post("/jpa/direct/transaction")
                    .then()
                    .statusCode(500);

            RestAssured.get("/jpa/fruit/named/Grapes")
                    .then()
                    .statusCode(200)
                    .body("name", contains("Grapes"));

            RestAssured.get("/jpa/fruit/named/" + rejected.getName())
                    .then()
                    .statusCode(200)
                    .body("$.size()", is(0));
        } finally {
            RestAssured.delete("/jpa/fruit/" + acceptedId)
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    public void testJpaMessageIdRepository() {
        IntStream.of(1, 2, 1, 3, 2).forEach((id) -> {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(JsonbBuilder.create().toJson(new Fruit(Integer.toString(id))))
                    .header("messageId", id)
                    .post("/jpa/direct/idempotent")
                    .then()
                    .statusCode(200);
        });

        RestAssured.given()
                .contentType(ContentType.JSON)
                .post("/jpa/direct/idempotentLog")
                .then()
                .statusCode(200)
                .body("$.size()", is(3))
                .body("messageId", containsInAnyOrder("1", "2", "3"))
                .body("processorName", hasItems("idempotentProcessor"));

        RestAssured.get("/jpa/mock/idempotent")
                .then()
                .statusCode(200)
                .body("size()", is(3));
    }

    public Fruit findFruit(int id) {
        return JsonbBuilder.create().fromJson(
                RestAssured.get("/jpa/fruit/" + id)
                        .then()
                        .statusCode(200)
                        .extract().body().asString(),
                Fruit.class);
    }
}
