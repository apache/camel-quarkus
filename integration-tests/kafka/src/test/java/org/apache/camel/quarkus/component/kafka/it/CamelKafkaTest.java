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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class CamelKafkaTest {

    //@Test
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

    //@Test
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

    //@Test
    void testQuarkusKafkaClientFactoryNotConfigured() {
        // quarkus-kubernetes-service-binding is not on the classpath so there should be no
        // custom KafkaClientFactory configured in the registry.
        RestAssured.get("/kafka/custom/client/factory/missing")
                .then()
                .statusCode(200)
                .body(is("true"));
    }
}
