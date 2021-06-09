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

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "AZURE_STORAGE_ACCOUNT_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_STORAGE_ACCOUNT_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_EVENT_HUBS_BLOB_CONTAINER_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_EVENT_HUBS_CONNECTION_STRING", matches = ".+")
@QuarkusTest
class AzureEventhubsTest {

    private static final Logger LOG = Logger.getLogger(AzureEventhubsTest.class);

    @Test
    public void roundTrip() {
        final String messageBody = RandomStringUtils.randomAlphabetic(30);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(messageBody)
                .post("/azure-eventhubs/send-events")
                .then()
                .statusCode(201);

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(() -> {
            final String body = RestAssured.given()
                    .get("/azure-eventhubs/receive-events")
                    .then()
                    .extract().body().asString();
            LOG.infof("Expected message body: '%s'; actual: '%s'", messageBody, body);
            return body != null && body.contains(messageBody);
        });

    }
}
