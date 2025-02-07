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
package org.apache.camel.quarkus.component.azure.key.vault.it;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubConsumerAsyncClient;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.models.EventPosition;
import io.restassured.RestAssured;
import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.hamcrest.Matchers.is;

// Azure Key Vault is not supported by Azurite https://github.com/Azure/Azurite/issues/619
abstract class AbstractAzureKeyVaultContextReloadTest {

    private static final Logger LOG = Logger.getLogger(AbstractAzureKeyVaultContextReloadTest.class);
    private static final String SECRET_NAME_FOR_REFRESH_PREFIX = "cq-secret-context-refresh-";
    private static final String AZURE_VAULT_EVENT_HUBS_CONNECTION_STRING = "AZURE_VAULT_EVENT_HUBS_CONNECTION_STRING";

    private final boolean useIdentity;

    public AbstractAzureKeyVaultContextReloadTest(boolean useIdentity) {
        this.useIdentity = useIdentity;
    }

    private String generateRefreshEvent(String secretName) {
        return "[{\n" +
                "  \"subject\": \"" + SECRET_NAME_FOR_REFRESH_PREFIX + (useIdentity ? "Identity-" : "") + ".*\",\n" +
                "  \"eventType\": \"Microsoft.KeyVault.SecretNewVersionCreated\"\n" +
                "}]";
    }

    @Test
    void contextReload() {
        String secretName = SECRET_NAME_FOR_REFRESH_PREFIX + (useIdentity ? "Identity-" : "") + UUID.randomUUID();
        String secretValue = "Hello Camel Quarkus Azure Key Vault From Refresh";
        try {
            // Create secret
            RestAssured.given()
                    .body(secretValue)
                    .post("/azure-key-vault/secret/true/{secretName}", secretName)
                    .then()
                    .statusCode(200)
                    .body(is(secretName));

            // Retrieve secret
            RestAssured.given()
                    .get("/azure-key-vault/secret/true/{secretName}", secretName)
                    .then()
                    .statusCode(200);

            //force reload by sending a msg
            try (EventHubProducerClient client = new EventHubClientBuilder()
                    .connectionString(System.getenv(AZURE_VAULT_EVENT_HUBS_CONNECTION_STRING))
                    .buildProducerClient()) {

                EventData eventData = new EventData(generateRefreshEvent(secretName).getBytes());
                List<EventData> finalEventData = new LinkedList<>();
                finalEventData.add(eventData);
                client.send(finalEventData);
            } catch (Exception e) {
                LOG.info("Failed to send a refresh message", e);
            }

            //await context reload
            Awaitility.await().pollInterval(10, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        RestAssured.get("/azure-key-vault/context/reload")
                                .then()
                                .statusCode(200)
                                .body(CoreMatchers.is("true"));
                    });
        } finally {

            //move cursor of events to ignore old ones (old events are deleted after 1 hour)
            try {
                String connectionString = System.getenv(AZURE_VAULT_EVENT_HUBS_CONNECTION_STRING);
                String consumerGroup = EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME;

                try (EventHubConsumerAsyncClient consumer = new EventHubClientBuilder()
                        .connectionString(connectionString)
                        .consumerGroup(consumerGroup)
                        .buildAsyncConsumerClient()) {

                    // Move consumer to the latest position, skipping old messages
                    consumer.receiveFromPartition("0", EventPosition.latest())
                            .subscribe(event -> {
                                System.out.println("Processing new event: " + event.toString());
                            }, error -> {
                                System.err.println("Error receiving events: " + error);
                            });
                }
            } catch (Exception e) {
                LOG.info("Failed to clear event hub.", e);
            }

            AzureKeyVaultUtil.deleteSecretImmediately(secretName);
        }
    }
}
