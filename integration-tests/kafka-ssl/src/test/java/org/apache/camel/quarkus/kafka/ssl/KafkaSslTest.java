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
package org.apache.camel.quarkus.kafka.ssl;

import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(KafkaSslTestResource.class)
public class KafkaSslTest {

    @Test
    void testKafkaBridge() {
        String body = UUID.randomUUID().toString();

        RestAssured.given()
                .contentType("text/plain")
                .body(body)
                .post("/kafka-ssl/inbound")
                .then()
                .statusCode(200);

        JsonPath result = RestAssured.given()
                .get("/kafka-ssl/outbound")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(result.getString("topicName")).isEqualTo("outbound");
        assertThat(result.getString("body")).isEqualTo(body);
    }

    @Test
    void testQuarkusKafkaClientFactoryNotConfigured() {
        // quarkus-kubernetes-service-binding is on the classpath but configuration merging is disabled
        // so there should be no custom KafkaClientFactory in the registry.
        RestAssured.get("/kafka-ssl/custom/client/factory/missing")
                .then()
                .statusCode(200)
                .body(is("true"));
    }
}
