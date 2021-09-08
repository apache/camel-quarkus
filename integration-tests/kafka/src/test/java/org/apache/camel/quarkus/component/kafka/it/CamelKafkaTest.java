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
package org.apache.camel.quarkus.component.kafka.it;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class CamelKafkaTest {

    @Test
    void testKafkaBridge() {
        String body = UUID.randomUUID().toString();

        given()
                .contentType("text/plain")
                .body(body)
                .post("/kafka/inbound")
                .then()
                .statusCode(200);

        JsonPath result = given()
                .get("/kafka/outbound")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(result.getString("topicName")).isEqualTo("outbound");
        assertThat(result.getString("body")).isEqualTo(body);
    }

    @Test
    void testIndempotent() {

        for (int i = 0; i < 10; i++) {
            int id = i % 5;
            given()
                    .contentType(ContentType.JSON)
                    .body("Test message")
                    .put("/kafka/idempotent/" + id)
                    .then()
                    .statusCode(202);
        }

        List<String> body = RestAssured.get("/kafka/idempotent").then().extract().body().as(List.class);
        assertEquals(5, body.size());

    }

    @Test
    void testQuarkusKafkaClientFactoryNotConfigured() {
        // quarkus-kubernetes-service-binding is not on the classpath so there should be no
        // custom KafkaClientFactory configured in the registry.
        RestAssured.get("/kafka/custom/client/factory/missing")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void testManualCommit() {
        String body1 = UUID.randomUUID().toString();

        // test consuming first message with manual auto-commit
        // send message that should be consumed by route with routeId = foo
        given()
                .contentType("text/plain")
                .body(body1)
                .post("/kafka/manual-commit-topic")
                .then()
                .statusCode(200);

        // make sure the message has been consumed
        given()
                .contentType("text/plain")
                .body(body1)
                .get("/kafka/seda/foo")
                .then()
                .body(equalTo(body1));

        String body2 = UUID.randomUUID().toString();

        given()
                .contentType("text/plain")
                .body(body2)
                .post("/kafka/manual-commit-topic")
                .then()
                .statusCode(200);

        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(30, TimeUnit.SECONDS).until(() -> {

            // make sure the message has been consumed
            String result = given()
                    .contentType("text/plain")
                    .get("/kafka/seda/foo")
                    .asString();
            return body2.equals(result);
        });

        // stop foo route
        given()
                .contentType("text/plain")
                .body(body1)
                .post("/kafka/foo/stop")
                .then()
                .statusCode(200);

        // start again the foo route
        given()
                .contentType("text/plain")
                .body(body1)
                .post("/kafka/foo/start")
                .then()
                .statusCode(200);

        // Make sure the second message is redelivered
        Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS).atMost(30, TimeUnit.SECONDS).until(() -> {

            // make sure the message has been consumed
            String result = given()
                    .contentType("text/plain")
                    .get("/kafka/seda/foo")
                    .asString();
            return body2.equals(result);
        });
    }

    @Test
    void testSerializers() {
        given()
                .contentType("text/json")
                .body(95.59F)
                .post("/kafka/price/1")
                .then()
                .statusCode(200);

        // make sure the message has been consumed
        given()
                .contentType("text/json")
                .get("/kafka/price")
                .then()
                .body("key", equalTo(1))
                .body("price", equalTo(95.59F));
    }

    @Test
    void testHeaderPropagation() throws InterruptedException {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("hello world")
                .post("/kafka/propagate/5")
                .then()
                .statusCode(200);

        // make sure the message has been consumed, and that the id put in the header has been propagated
        given()
                .contentType("text/json")
                .get("/kafka/propagate")
                .then()
                .body("id", equalTo("5"))
                .body("message", equalTo("hello world"));
    }
}
