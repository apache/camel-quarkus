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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.kafka.InjectKafka;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
@TestProfile(KafkaHealthCheckProfile.class)
public class CamelKafkaHealthCheckTest {

    @InjectKafka
    KafkaContainer container;

    @Test
    void testHealthCheck() {
        RestAssured.when().get("/q/health").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"));

        // stop the kafka container to test health-check DOWN
        container.stop();

        RestAssured.when().get("/q/health").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("DOWN"),
                        "checks.findAll { it.name.contains('kafka') }.status",
                        contains("DOWN", "DOWN", "DOWN", "DOWN", "DOWN"),
                        "checks.findAll { it.name.contains('kafka') }.data.topic",
                        containsInAnyOrder("outbound", "inbound", "test-propagation", "test-serializer", "manual-commit-topic"),
                        "checks.findAll { it.name.contains('kafka') }.data.'client.id' ",
                        contains(notNullValue(), notNullValue(), notNullValue(), notNullValue(), notNullValue()));
    }
}
