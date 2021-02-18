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

import javax.enterprise.context.ApplicationScoped;

import com.azure.core.amqp.AmqpTransportType;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AzureEventhubsRoutes extends RouteBuilder {

    @ConfigProperty(name = "azure.storage.account-name")
    String azureStorageAccountName;

    @ConfigProperty(name = "azure.storage.account-key")
    String azureStorageAccountKey;

    @ConfigProperty(name = "azure.event.hubs.connection.string")
    String connectionString;

    @ConfigProperty(name = "azure.blob.container.name")
    String azureBlobContainerName;

    @Override
    public void configure() throws Exception {
        from("azure-eventhubs:?connectionString=RAW(" + connectionString
                + ")&blobAccountName=RAW(" + azureStorageAccountName
                + ")&blobAccessKey=RAW(" + azureStorageAccountKey
                + ")&blobContainerName=RAW(" + azureBlobContainerName + ")&amqpTransportType="
                + AmqpTransportType.AMQP)
                        .to("mock:azure-consumed");

    }

}
