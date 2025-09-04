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
package org.apache.camel.quarkus.component.azure.servicebus.it;

import java.net.URI;
import java.util.Optional;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.core.credential.TokenCredential;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.component.azure.servicebus.CredentialType;
import org.apache.camel.component.azure.servicebus.ServiceBusType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AzureServiceBusRoutes extends EndpointRouteBuilder {
    @ConfigProperty(name = "azure.servicebus.connection.string")
    Optional<String> connectionString;

    @ConfigProperty(name = "azure.servicebus.queue.name")
    Optional<String> queueName;

    @ConfigProperty(name = "azure.servicebus.topic.name")
    Optional<String> topicName;

    @ConfigProperty(name = "azure.servicebus.topic.subscription.name")
    Optional<String> topicSubscriptionName;

    @Inject
    @Named("serviceBusTokenCredential")
    TokenCredential tokenCredential;

    @Inject
    @Named("namespaceURI")
    URI namespaceURI;

    @Override
    public void configure() {
        if (AzureServiceBusHelper.isMinimumConfigurationAvailable()) {
            String mockEndpointUri = "${header.serviceBusType}-${header.destination}-${header.transportType}-${header.payloadType}-results";

            // Simple queue consumer
            from(azureServicebus(queueName.get())
                    .connectionString("RAW(" + connectionString.get() + ")"))
                    .routeId("servicebus-queue-consumer-" + AmqpTransportType.AMQP)
                    .autoStartup(false)
                    .toD(mock(mockEndpointUri));

            // Consume from queue with web socket transport
            from(azureServicebus(queueName.get())
                    .connectionString("RAW(" + connectionString.get() + ")")
                    .amqpTransportType(AmqpTransportType.AMQP_WEB_SOCKETS))
                    .autoStartup(false)
                    .routeId("servicebus-queue-consumer-" + AmqpTransportType.AMQP_WEB_SOCKETS)
                    .toD(mock(mockEndpointUri));

            // Queue consumer with a custom client
            from(azureServicebus(queueName.get())
                    .processorClient("#customProcessorClient"))
                    .autoStartup(false)
                    .routeId("servicebus-queue-consumer-custom-processor")
                    .toD(AzureServiceBusProducers.MOCK_ENDPOINT_URI);

            // Queue consumer using TOKEN_CREDENTIAL authentication
            from(azureServicebus(queueName.get())
                    .credentialType(CredentialType.TOKEN_CREDENTIAL)
                    .fullyQualifiedNamespace(namespaceURI.getHost())
                    .tokenCredential(tokenCredential))
                    .autoStartup(false)
                    .routeId("servicebus-queue-consumer-token-credential")
                    .to("mock:servicebus-token-credential-results");

            // Queue consumer for scheduled messages
            from(azureServicebus(queueName.get())
                    .connectionString("RAW(" + connectionString.get() + ")"))
                    .routeId("servicebus-queue-scheduled-consumer")
                    .autoStartup(false)
                    .to("mock:servicebus-queue-scheduled-consumer-results");

            if (AzureServiceBusHelper.isAzureServiceBusTopicConfigPresent()) {
                // Simple topic consumer
                from(azureServicebus(topicName.get())
                        .serviceBusType(ServiceBusType.topic)
                        .subscriptionName(topicSubscriptionName.get())
                        .connectionString("RAW(" + connectionString.get() + ")"))
                        .routeId("servicebus-topic-consumer-" + AmqpTransportType.AMQP)
                        .autoStartup(false)
                        .toD(mock(mockEndpointUri));

                // Consume from topic with web socket transport
                from(azureServicebus(topicName.get())
                        .serviceBusType(ServiceBusType.topic)
                        .subscriptionName(topicSubscriptionName.get())
                        .connectionString("RAW(" + connectionString.get() + ")")
                        .amqpTransportType(AmqpTransportType.AMQP_WEB_SOCKETS))
                        .autoStartup(false)
                        .routeId("servicebus-topic-consumer-" + AmqpTransportType.AMQP_WEB_SOCKETS)
                        .toD(mock(mockEndpointUri));
            }

            // Produces messages to a given destination using the specified transport type
            from(direct("send-message"))
                    .toD(azureServicebus("${header.destination}")
                            .connectionString("RAW(" + connectionString.get() + ")")
                            .serviceBusType("${header.serviceBusType}")
                            .amqpTransportType("${header.transportType}"));

            // Produces messages to a queue using a custom sender client
            from(direct("send-message-custom-client"))
                    .to(azureServicebus(queueName.get())
                            .senderClient("#customSenderClient"));

            // Produces messages using TOKEN_CREDENTIAL authentication
            from(direct("token-credential"))
                    .to(azureServicebus(queueName.get())
                            .credentialType(CredentialType.TOKEN_CREDENTIAL)
                            .fullyQualifiedNamespace(namespaceURI.getHost())
                            .tokenCredential(tokenCredential));

            // Produce scheduled messages
            from(direct("scheduled"))
                    .to(azureServicebus(queueName.get())
                            .connectionString("RAW(" + connectionString.get() + ")")
                            .producerOperation("scheduleMessages"));

            if (AzureServiceBusHelper.isAzureIdentityCredentialsAvailable()) {
                // Queue consumer using AZURE_IDENTITY authentication
                from(azureServicebus(queueName.get())
                        .credentialType(CredentialType.AZURE_IDENTITY)
                        .fullyQualifiedNamespace(namespaceURI.getHost()))
                        .autoStartup(false)
                        .routeId("servicebus-queue-consumer-azure-identity")
                        .to("mock:servicebus-azure-identity-results");

                // Produce messages using AZURE_IDENTITY credential type
                from(direct("azure-identity"))
                        .to(azureServicebus(queueName.get())
                                .credentialType(CredentialType.AZURE_IDENTITY)
                                .fullyQualifiedNamespace(namespaceURI.getHost()));
            }
        }
    }
}
