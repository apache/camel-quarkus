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

import java.util.Optional;

import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AzureEventhubsMockedRoutes extends EndpointRouteBuilder {
    @ConfigProperty(name = "azure.storage.account-name")
    String azureStorageAccountName;

    @ConfigProperty(name = "azure.storage.account-key")
    String azureStorageAccountKey;

    @ConfigProperty(name = "azure.blob.service.url")
    Optional<String> azureBlobServiceUrl;

    @ConfigProperty(name = "azure.event.hubs.connection.string")
    Optional<String> connectionString;

    @ConfigProperty(name = "azure.event.hubs.blob.container.name")
    Optional<String> azureBlobContainerName;

    @Override
    public void configure() {
        if (MockBackendUtils.startMockBackend() && azureBlobServiceUrl.isPresent() && azureBlobContainerName.isPresent()) {
            BlobContainerAsyncClient blobClient = new BlobContainerClientBuilder()
                    .endpoint(azureBlobServiceUrl.get())
                    .containerName(azureBlobContainerName.get())
                    .credential(new StorageSharedKeyCredential(azureStorageAccountName, azureStorageAccountKey))
                    .buildAsyncClient();

            blobClient.createIfNotExists().block();

            from(azureEventhubs("")
                    .connectionString(connectionString.get())
                    .checkpointStore(new BlobCheckpointStore(blobClient)))
                    .routeId("eventhubs-mocked-consumer")
                    .autoStartup(false)
                    .log("Consumed event payload ${body} from partition ${header.CamelAzureEventHubsPartitionId}")
                    .choice()
                    .when(simple("${header.CamelAzureEventHubsPartitionId} == 0"))
                    .to("mock:partition-0-mocked-results")
                    .otherwise()
                    .log("Message received from unexpected partition id ${header.CamelAzureEventHubsPartitionId}");

            from("direct:sendEvent")
                    .log("Sending event ${body}")
                    .to(azureEventhubs("")
                            .connectionString(connectionString.get()));
        }
    }
}
