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
package org.apache.camel.quarkus.component.knative.event.consumer.it;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.cloudevents.CloudEvents;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.apache.camel.quarkus.component.knative.event.consumer.it.KnativeEventConsumerTest.*;

@TestProfile(KnativeEventConsumerTestProfile.class)
@QuarkusTest
public class KnativeEventConsumerTest {

    @Test
    void consumeFromBroker() {
        given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.TEXT_PLAIN)
                .header("ce-type", "event-test")
                .header("ce-specversion", CloudEvents.v1_0.version())
                .header("ce-id", UUID.randomUUID())
                .header("ce-time", "2018-04-05T17:31:00Z")
                .header("ce-source", "camel-event-test")
                .body("Hello World - Testing Knative Broker Camel consumer")
                .when()
                .post("/")
                .then()
                .statusCode(204);

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            final String body = given()
                    .get("/knative-event-consumer")
                    .then()
                    .extract().body().asString();
            return body != null && body.contains("Hello World - Testing Knative Broker Camel consumer");
        });
    }

    public static final class KnativeEventConsumerTestProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "knative-event-consumer";
        }
    }
}
