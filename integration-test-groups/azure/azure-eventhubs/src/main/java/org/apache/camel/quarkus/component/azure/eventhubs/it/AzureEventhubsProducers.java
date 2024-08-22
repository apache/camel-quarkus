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

import com.azure.core.amqp.implementation.ConnectionStringProperties;
import com.azure.core.credential.TokenCredential;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.azure.messaging.eventhubs.implementation.ClientConstants;
import com.azure.messaging.eventhubs.implementation.EventHubSharedKeyCredential;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class AzureEventhubsProducers {
    @ConfigProperty(name = "azure.event.hubs.connection.string")
    Optional<String> connectionString;

    @Named("connectionStringTokenCredential")
    TokenCredential tokenCredential() {
        if (connectionString.isPresent()) {
            ConnectionStringProperties properties = new ConnectionStringProperties(connectionString.get());
            TokenCredential tokenCredential;
            if (properties.getSharedAccessSignature() == null) {
                tokenCredential = new EventHubSharedKeyCredential(properties.getSharedAccessKeyName(),
                        properties.getSharedAccessKey(), ClientConstants.TOKEN_VALIDITY);
            } else {
                tokenCredential = new EventHubSharedKeyCredential(properties.getSharedAccessSignature());
            }
            return tokenCredential;
        }
        return null;
    }

    @Named("eventHubClient")
    EventHubProducerAsyncClient eventHubClient() {
        return connectionString.map(connection -> new EventHubClientBuilder()
                .connectionString(connection)
                .buildAsyncProducerClient())
                .orElse(null);
    }
}
