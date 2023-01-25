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

import com.azure.core.amqp.AmqpTransportType;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AzureEventhubsRoutes extends RouteBuilder {

    @ConfigProperty(name = "azure.storage.account-name")
    String azureStorageAccountName;

    @ConfigProperty(name = "azure.storage.account-key")
    String azureStorageAccountKey;

    @ConfigProperty(name = "azure.event.hubs.connection.string")
    Optional<String> connectionString;

    @ConfigProperty(name = "azure.event.hubs.blob.container.name")
    Optional<String> azureBlobContainerName;

    @ConfigProperty(name = "camel.quarkus.start.mock.backend", defaultValue = "true")
    boolean startMockBackend;

    @Override
    public void configure() throws Exception {
        if (connectionString.isPresent() && azureBlobContainerName.isPresent()) {
            from("azure-eventhubs:?connectionString=RAW(" + connectionString.get()
                    + ")&blobAccountName=RAW(" + azureStorageAccountName
                    + ")&blobAccessKey=RAW(" + azureStorageAccountKey
                    + ")&blobContainerName=RAW(" + azureBlobContainerName.get() + ")&amqpTransportType="
                    + AmqpTransportType.AMQP)
                            .to("mock:azure-consumed");
        } else if (!startMockBackend) {
            throw new IllegalStateException(
                    "azure.event.hubs.connection.string and azure.event.hubs.blob.container.name must be set when camel.quarkus.start.mock.backend == false");
        }
    }

}
