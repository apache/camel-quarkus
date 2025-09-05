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

import java.util.Map;
import java.util.Optional;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.core.credential.TokenCredential;
import com.azure.messaging.eventhubs.models.EventPosition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.component.azure.eventhubs.CredentialType;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AzureEventhubsRoutes extends EndpointRouteBuilder {

    @ConfigProperty(name = "azure.storage.account-name")
    String azureStorageAccountName;

    @ConfigProperty(name = "azure.storage.account-key")
    String azureStorageAccountKey;

    @ConfigProperty(name = "azure.event.hubs.connection.string")
    Optional<String> connectionString;

    @ConfigProperty(name = "azure.event.hubs.blob.container.name")
    Optional<String> azureBlobContainerName;

    @Inject
    @Named("eventHubsTokenCredential")
    TokenCredential tokenCredential;

    @Override
    public void configure() {
        if (!MockBackendUtils.startMockBackend() && !AzureCredentialsHelper.isMinimumConfigurationAvailable()) {
            throw new IllegalStateException(
                    "Configuration properties azure.event.hubs.connection.string, azure.event.hubs.blob.container.name & azure.storage.account-key must be set when camel.quarkus.start.mock.backend == false");
        }

        if (AzureCredentialsHelper.isMinimumConfigurationAvailable()) {
            Map<String, String> connectionProperties = AzureCredentialsHelper.parseConnectionString(connectionString.get());
            String eventHubsPath = "%s/%s".formatted(connectionProperties.get("Namespace"),
                    connectionProperties.get("EntityPath"));

            // Consumes EventHub messages and routes them based on which partition they are associated with
            from(azureEventhubs("")
                    .connectionString("RAW(" + connectionString.get() + ")")
                    .blobAccountName(azureStorageAccountName)
                    .blobAccessKey("RAW(" + azureStorageAccountKey + ")")
                    .blobContainerName(azureBlobContainerName.get()))
                    .routeId("eventhubs-consumer")
                    .autoStartup(false)
                    .log("Consumed event payload ${body} from partition ${header.CamelAzureEventHubsPartitionId}")
                    .choice()
                    .when(simple("${header.CamelAzureEventHubsPartitionId} == 0"))
                    .to("mock:partition-0-results")
                    .when(simple("${header.CamelAzureEventHubsPartitionId} == 1"))
                    .to("mock:partition-1-results")
                    .otherwise()
                    .log("Message received from unexpected partition id ${header.CamelAzureEventHubsPartitionId}");

            // Consumes events from partition 2 with InMemoryCheckpointStore
            from(azureEventhubs("")
                    .connectionString("RAW(" + connectionString.get() + ")")
                    .checkpointStore(new InMemoryCheckpointStore()))
                    .routeId("eventhubs-consumer-custom-checkpoint-store")
                    .autoStartup(false)
                    .log("Consumed event payload ${body} from partition ${header.CamelAzureEventHubsPartitionId} with InMemoryCheckpointStore")
                    .choice()
                    .when(simple("${header.CamelAzureEventHubsPartitionId} == 2"))
                    .to("mock:partition-2-initial-results")
                    .otherwise()
                    .log("Message received from unexpected partition id ${header.CamelAzureEventHubsPartitionId}");

            // Reads all events sent to partition 2 from the beginning
            from(azureEventhubs("")
                    .connectionString("RAW(" + connectionString.get() + ")")
                    .checkpointStore(new InMemoryCheckpointStore())
                    .eventPosition(Map.of("2", EventPosition.earliest())))
                    .routeId("eventhubs-consumer-with-event-position")
                    .autoStartup(false)
                    .log("Consumed event payload ${body} from partition ${header.CamelAzureEventHubsPartitionId} from EventPosition.earliest")
                    .choice()
                    .when(simple("${header.CamelAzureEventHubsPartitionId} == 2"))
                    .to("mock:partition-2-event-position-results")
                    .otherwise()
                    .log("Message received from unexpected partition id ${header.CamelAzureEventHubsPartitionId}");

            // Consumes events from partition 3 using a custom TokenCredential
            from(azureEventhubs(eventHubsPath)
                    .credentialType(CredentialType.TOKEN_CREDENTIAL)
                    .tokenCredential(tokenCredential)
                    .blobAccountName(azureStorageAccountName)
                    .blobAccessKey("RAW(" + azureStorageAccountKey + ")")
                    .blobContainerName(azureBlobContainerName.get()))
                    .routeId("eventhubs-consumer-custom-token-credential")
                    .autoStartup(false)
                    .log("Consumed event payload ${body} from partition ${header.CamelAzureEventHubsPartitionId} with TokenCredential")
                    .choice()
                    .when(simple("${header.CamelAzureEventHubsPartitionId} == 3"))
                    .to("mock:partition-3-results")
                    .otherwise()
                    .log("Message received from unexpected partition id ${header.CamelAzureEventHubsPartitionId}");

            // Consumes events from partition 4 using WS transport
            from(azureEventhubs("")
                    .connectionString("RAW(" + connectionString.get() + ")")
                    .blobAccountName(azureStorageAccountName)
                    .blobAccessKey("RAW(" + azureStorageAccountKey + ")")
                    .blobContainerName(azureBlobContainerName.get())
                    .amqpTransportType(AmqpTransportType.AMQP_WEB_SOCKETS))
                    .routeId("eventhubs-consumer-with-amqp-ws-transport")
                    .autoStartup(false)
                    .choice()
                    .when(simple("${header.CamelAzureEventHubsPartitionId} == 4"))
                    .to("mock:partition-4-ws-transport-results")
                    .otherwise()
                    .log("Message received from unexpected partition id ${header.CamelAzureEventHubsPartitionId}");

            from("direct:sendEvent")
                    .to(azureEventhubs("")
                            .connectionString("RAW(" + connectionString.get() + ")"));

            from("direct:sendEventUsingAmqpWebSockets")
                    .to(azureEventhubs("")
                            .connectionString("RAW(" + connectionString.get() + ")")
                            .amqpTransportType(AmqpTransportType.AMQP_WEB_SOCKETS));

            from("direct:sendEventUsingTokenCredential")
                    .to(azureEventhubs(eventHubsPath)
                            .credentialType(CredentialType.TOKEN_CREDENTIAL)
                            .tokenCredential(tokenCredential));

            // Consumes EventHub messages that are produced by the custom client in direct:sendEventUsingCustomClient
            from(azureEventhubs("")
                    .connectionString("RAW(" + connectionString.get() + ")")
                    .blobAccountName(azureStorageAccountName)
                    .blobAccessKey("RAW(" + azureStorageAccountKey + ")")
                    .blobContainerName(azureBlobContainerName.get()))
                    .routeId("eventhubs-consumer-for-custom-client")
                    .autoStartup(false)
                    .log("Consumed event payload ${body} from partition ${header.CamelAzureEventHubsPartitionId}")
                    .choice()
                    .when(simple("${header.CamelAzureEventHubsPartitionId} == 0"))
                    .to("mock:partition-0-custom-client-results")
                    .otherwise()
                    .log("Message received from unexpected partition id ${header.CamelAzureEventHubsPartitionId}");

            from("direct:sendEventUsingCustomClient")
                    .to(azureEventhubs("")
                            .producerAsyncClient("#eventHubClient"));

            // Consumes using an auto-generated connection string from the shared access configuration
            from(azureEventhubs(eventHubsPath)
                    .sharedAccessName(connectionProperties.get("SharedAccessKey"))
                    .sharedAccessKey("RAW(" + connectionProperties.get("SharedAccessKeyValue") + ")")
                    .blobAccountName(azureStorageAccountName)
                    .blobAccessKey("RAW(" + azureStorageAccountKey + ")")
                    .blobContainerName(azureBlobContainerName.get()))
                    .routeId("eventhubs-consumer-generated-connection-string")
                    .autoStartup(false)
                    .log("Consumed event payload ${body} from partition ${header.CamelAzureEventHubsPartitionId}")
                    .choice()
                    .when(simple("${header.CamelAzureEventHubsPartitionId} == 0"))
                    .to("mock:partition-0-generated-connection-string-results")
                    .otherwise()
                    .log("Message received from unexpected partition id ${header.CamelAzureEventHubsPartitionId}");

            from("direct:sendEventWithGeneratedConnectionString")
                    .to(azureEventhubs(eventHubsPath)
                            .sharedAccessName(connectionProperties.get("SharedAccessKey"))
                            .sharedAccessKey("RAW(" + connectionProperties.get("SharedAccessKeyValue") + ")"));

            if (AzureCredentialsHelper.isAzureIdentityCredentialsAvailable()) {
                // Consumes events from partition 4 using AZURE_IDENTITY credential type
                from(azureEventhubs(eventHubsPath)
                        .credentialType(CredentialType.AZURE_IDENTITY)
                        .blobAccountName(azureStorageAccountName)
                        .blobAccessKey("RAW(" + azureStorageAccountKey + ")")
                        .blobContainerName(azureBlobContainerName.get()))
                        .routeId("eventhubs-consumer-azure-identity-credential")
                        .autoStartup(false)
                        .log("Consumed event payload ${body} from partition ${header.CamelAzureEventHubsPartitionId} with TokenCredential")
                        .choice()
                        .when(simple("${header.CamelAzureEventHubsPartitionId} == 4"))
                        .to("mock:partition-4-results")
                        .otherwise()
                        .log("Message received from unexpected partition id ${header.CamelAzureEventHubsPartitionId}");

                from("direct:sendEventUsingAzureIdentity")
                        .to(azureEventhubs(eventHubsPath)
                                .credentialType(CredentialType.AZURE_IDENTITY));
            }
        }
    }
}
