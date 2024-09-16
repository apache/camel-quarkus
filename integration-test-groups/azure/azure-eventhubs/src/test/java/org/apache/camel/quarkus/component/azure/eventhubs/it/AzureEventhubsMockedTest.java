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
package org.apache.camel.quarkus.component.azure.eventhubs.it;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.EnabledIf;
import org.apache.camel.quarkus.test.mock.backend.MockBackendEnabled;
import org.apache.camel.quarkus.test.support.azure.AzureStorageTestResource;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@WithTestResource(value = AzureStorageTestResource.class, initArgs = {
        @ResourceArg(name = "eventHubs", value = "true")
})
class AzureEventhubsMockedTest {
    @EnabledIf({ MockBackendEnabled.class })
    @Test
    void produceConsumeEvents() {
        try {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-mocked-consumer/start")
                    .then()
                    .statusCode(204);

            final String messageBody = UUID.randomUUID().toString();

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(messageBody)
                    .post("/azure-eventhubs/send-event/0")
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                RestAssured.given()
                        .queryParam("endpointUri", "mock:partition-0-mocked-results")
                        .body(messageBody)
                        .get("/azure-eventhubs/receive-event")
                        .then()
                        .statusCode(200)
                        .body(
                                "body", is(messageBody),
                                "headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "headers.CamelAzureEventHubsOffset", greaterThanOrEqualTo(0),
                                "headers.CamelAzureEventHubsPartitionId", is("0"),
                                "headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0));
            });
        } finally {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-mocked-consumer/stop")
                    .then()
                    .statusCode(204);
        }
    }
}
