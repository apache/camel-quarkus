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

import com.azure.core.amqp.implementation.ConnectionStringProperties;
import com.azure.core.credential.TokenCredential;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.implementation.ServiceBusSharedKeyCredential;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.AsyncCallback;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.azure.servicebus.ServiceBusConstants;
import org.apache.camel.component.azure.servicebus.ServiceBusConsumer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AzureServiceBusProducers {
    public static final String MOCK_ENDPOINT_URI = "mock:customProcessorClientResults";
    private static final Logger LOG = Logger.getLogger(AzureServiceBusProducers.class);

    @ConfigProperty(name = "azure.servicebus.connection.string")
    Optional<String> connectionString;

    @ConfigProperty(name = "azure.servicebus.queue.name")
    Optional<String> queueName;

    @Inject
    CamelContext context;

    @Named
    ServiceBusProcessorClient customProcessorClient() {
        if (connectionString.isPresent() && queueName.isPresent()) {
            return new ServiceBusClientBuilder()
                    .connectionString(connectionString.get())
                    .processor()
                    .queueName(queueName.get())
                    .processMessage(messageContext -> {
                        // Hook into consumer processing to make the message propagate through the route
                        ServiceBusConsumer consumer = (ServiceBusConsumer) context
                                .getRoute("servicebus-queue-consumer-custom-processor").getConsumer();
                        ServiceBusReceivedMessage serviceBusMessage = messageContext.getMessage();

                        Exchange exchange = consumer.createExchange(true);
                        Message message = exchange.getMessage();
                        message.setHeader(ServiceBusConstants.SEQUENCE_NUMBER, serviceBusMessage.getSequenceNumber());
                        message.setBody(serviceBusMessage.getBody());

                        AsyncCallback cb = consumer.defaultConsumerCallback(exchange, true);
                        consumer.getAsyncProcessor().process(exchange, cb);
                    })
                    .processError(errorContext -> LOG.errorf(errorContext.getException(),
                            "Error consuming from %s" + errorContext.getEntityPath()))
                    .buildProcessorClient();
        }
        return null;
    }

    @Named
    ServiceBusSenderClient customSenderClient() {
        if (connectionString.isPresent() && queueName.isPresent()) {
            return new ServiceBusClientBuilder()
                    .connectionString(connectionString.get())
                    .sender()
                    .queueName(queueName.get())
                    .buildClient();
        }
        return null;
    }

    @Named("serviceBusTokenCredential")
    TokenCredential tokenCredential() {
        if (connectionString.isPresent()) {
            ConnectionStringProperties properties = new ConnectionStringProperties(connectionString.get());
            TokenCredential tokenCredential;
            if (properties.getSharedAccessSignature() == null) {
                tokenCredential = new ServiceBusSharedKeyCredential(properties.getSharedAccessKeyName(),
                        properties.getSharedAccessKey());
            } else {
                tokenCredential = new ServiceBusSharedKeyCredential(properties.getSharedAccessSignature());
            }
            return tokenCredential;
        }
        return null;
    }

    @Named("namespaceURI")
    URI namespaceURI() {
        if (connectionString.isPresent()) {
            ConnectionStringProperties properties = new ConnectionStringProperties(connectionString.get());
            return properties.getEndpoint();
        }
        return null;
    }
}
