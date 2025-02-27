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
import java.util.concurrent.TimeUnit;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

// Azure Key Vault is not supported by Azurite https://github.com/Azure/Azurite/issues/619
abstract class AbstractAzureKeyVaultContextReloadTest {

    private static final Logger LOG = Logger.getLogger(AbstractAzureKeyVaultContextReloadTest.class);
    private static final String AZURE_VAULT_EVENT_HUBS_CONNECTION_STRING = "AZURE_VAULT_EVENT_HUBS_CONNECTION_STRING";

    private String generateRefreshEvent(String secretName) {
        return "[{\n" +
                "  \"subject\": \"" + secretName + "\",\n" +
                "  \"eventType\": \"Microsoft.KeyVault.SecretNewVersionCreated\"\n" +
                "}]";
    }

    @Test
    void contextReload() {
        String secretName = ConfigProvider.getConfig().getValue("camel.vault.azure.secrets", String.class).replace(".*", "");
        String secretValue = "Hello Camel Quarkus Azure Key Vault From Refresh";
        boolean reloadDetected = false;
        try {
            // Create secret
            RestAssured.given()
                    .body(secretValue)
                    .post("/azure-key-vault/secret/true/{secretName}", secretName)
                    .then()
                    .statusCode(200)
                    .body(is(secretName));
            LOG.infof("Secret created: %s", secretName);

            // Retrieve secret
            RestAssured.given()
                    .get("/azure-key-vault/secret/true/{secretName}", secretName)
                    .then()
                    .statusCode(200);
            LOG.info("Secret verified before refresh.");

            LOG.info("Wait some time for listener to be initialized");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            //force reload by sending a msg
            try (EventHubProducerClient client = new EventHubClientBuilder()
                    .connectionString(System.getenv(AZURE_VAULT_EVENT_HUBS_CONNECTION_STRING))
                    .buildProducerClient()) {

                EventData eventData = new EventData(generateRefreshEvent(secretName).getBytes());
                List<EventData> finalEventData = new LinkedList<>();
                finalEventData.add(eventData);
                LOG.info("Sending refresh event.");
                client.send(finalEventData);
            } catch (Exception e) {
                LOG.info("Failed to send a refresh message", e);
            }

            //await context reload
            Awaitility.await().pollInterval(10, TimeUnit.SECONDS).atMost(2, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        RestAssured.get("/azure-key-vault/context/reload")
                                .then()
                                .statusCode(200)
                                .body(CoreMatchers.is("true"));
                    });
            reloadDetected = true;
        } finally {
            // meant to be commented.
            // during development, it may be handy to mark eventhub as completely read. (in case the test is not reading all the messages by itself)
            // Please uncomment the rest of the code to make cursor reset to the latest position after test execution

            //            following code moves the cursor of the hub to the latest position, thus marking all events as read
            //            partition 0 is hardcoded (the resource script creates only one partition)
            //            by default this functionality should not be executed, as the eventbus might be shared for more tests/purposes
            //            even if event stays, it is removed by retention policy in some time (i.e. 1 hpr)
            //            try {
            //                String connectionString = System.getenv(AZURE_VAULT_EVENT_HUBS_CONNECTION_STRING);
            //                String consumerGroup = EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME;
            //
            //                try (EventHubConsumerAsyncClient consumer = new EventHubClientBuilder()
            //                        .connectionString(connectionString)
            //                        .consumerGroup(consumerGroup)
            //                        .buildAsyncConsumerClient()) {
            //
            //                    // Move consumer to the latest position, skipping old messages
            //                    consumer.receiveFromPartition("0", EventPosition.latest())
            //                            .subscribe(event -> {
            //                                System.out.println("Processing new event: " + event.toString());
            //                            }, error -> {
            //                                System.err.println("Error receiving events: " + error);
            //                            });
            //                }
            //            } catch (Exception e) {
            //                LOG.info("Failed to clear event hub.", e);
            //            }

            AzureKeyVaultUtil.deleteSecretImmediately(secretName, true);
        }
    }
}
