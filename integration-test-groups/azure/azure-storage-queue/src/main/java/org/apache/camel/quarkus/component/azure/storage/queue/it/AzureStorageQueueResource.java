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
package org.apache.camel.quarkus.component.azure.storage.queue.it;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.util.BinaryData;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import com.azure.storage.queue.models.PeekedMessageItem;
import com.azure.storage.queue.models.QueueItem;
import com.azure.storage.queue.models.QueueMessageItem;
import com.azure.storage.queue.models.QueuesSegmentOptions;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.azure.storage.queue.QueueConstants;
import org.apache.camel.component.azure.storage.queue.QueueOperationDefinition;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.azure.storage.queue.it.model.ExampleMessage;
import org.apache.camel.spi.RouteController;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/azure-storage-queue")
@ApplicationScoped
public class AzureStorageQueueResource {

    protected static final String QUEUE_NAME = "camel-quarkus-" + UUID.randomUUID().toString();

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @ConfigProperty(name = "azure.storage.account-name")
    String azureStorageAccountName;

    @ConfigProperty(name = "azure.storage.account-key")
    String azureStorageAccountKey;

    @ConfigProperty(name = "azure.queue.service.url")
    String azureQueueServiceUrl;

    @javax.enterprise.inject.Produces
    public QueueServiceClient createQueueClient() throws Exception {
        final StorageSharedKeyCredential credentials = new StorageSharedKeyCredential(azureStorageAccountName,
                azureStorageAccountKey);
        return new QueueServiceClientBuilder()
                .endpoint(azureQueueServiceUrl)
                .credential(credentials)
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .buildClient();

    }

    @Path("/queue/create")
    @POST
    public Response createQueue() throws Exception {
        producerTemplate.sendBody(componentUri(QueueOperationDefinition.createQueue),
                null);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/queue/read")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ExampleMessage> retrieveMessage() throws Exception {
        @SuppressWarnings("unchecked")
        List<QueueMessageItem> messages = producerTemplate.requestBody(
                componentUri(QueueOperationDefinition.receiveMessages),
                null, List.class);
        return messages.stream()
                .map(this::createMessage)
                .collect(Collectors.toList());
    }

    @Path("/queue/peek")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String peekOneMessage() throws Exception {
        @SuppressWarnings("unchecked")
        List<PeekedMessageItem> messages = producerTemplate.requestBodyAndHeader(
                componentUri(QueueOperationDefinition.peekMessages),
                null, QueueConstants.MAX_MESSAGES, 1,
                List.class);
        return messages.stream()
                .map(PeekedMessageItem::getBody)
                .map(BinaryData::toString)
                .collect(Collectors.joining("\n"));
    }

    @Path("/queue/message")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response addMessage(String message) throws Exception {
        producerTemplate.sendBody(
                componentUri(QueueOperationDefinition.sendMessage),
                message);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/queue/list")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String listQueues() throws Exception {
        QueuesSegmentOptions queuesSegmentOptions = new QueuesSegmentOptions();
        queuesSegmentOptions.setIncludeMetadata(true);
        @SuppressWarnings("unchecked")
        List<QueueItem> messages = producerTemplate.requestBodyAndHeader(
                componentUri(QueueOperationDefinition.listQueues),
                null, QueueConstants.QUEUES_SEGMENT_OPTIONS, queuesSegmentOptions, List.class);
        return messages.stream()
                .map(QueueItem::getName)
                .collect(Collectors.joining("\n"));
    }

    @Path("/queue/delete")
    @DELETE
    public Response deleteQueue() throws Exception {
        producerTemplate.sendBody(
                componentUri(QueueOperationDefinition.deleteQueue),
                null);
        return Response.noContent().build();
    }

    @Path("/queue/delete/{id}/{popReceipt}")
    @DELETE
    public Response deleteMessageById(@PathParam("id") String id, @PathParam("popReceipt") String popReceipt) throws Exception {
        var headers = new HashMap<String, Object>();
        headers.put(QueueConstants.MESSAGE_ID, id);
        headers.put(QueueConstants.POP_RECEIPT, popReceipt);
        headers.put(QueueConstants.VISIBILITY_TIMEOUT, Duration.ofMillis(10));
        producerTemplate.sendBodyAndHeaders(
                componentUri(QueueOperationDefinition.deleteMessage),
                null, headers);
        return Response.noContent().build();
    }

    @Path("/queue/update/{id}/{popReceipt}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response addMessage(@PathParam("id") String id, @PathParam("popReceipt") String popReceipt, String message)
            throws Exception {
        var headers = new HashMap<String, Object>();
        headers.put(QueueConstants.MESSAGE_ID, id);
        headers.put(QueueConstants.POP_RECEIPT, popReceipt);
        headers.put(QueueConstants.VISIBILITY_TIMEOUT, Duration.ofMillis(10));

        producerTemplate.sendBodyAndHeaders(
                componentUri(QueueOperationDefinition.updateMessage),
                message,
                headers);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/queue/clear")
    @GET
    public Response clearQueue() throws Exception {
        producerTemplate.sendBody(
                componentUri(QueueOperationDefinition.clearQueue),
                null);
        return Response.noContent().build();
    }

    @Path("/queue/consumer/{action}")
    @POST
    public Response modifyConsumerRouteState(@PathParam("action") String action) throws Exception {
        RouteController controller = context.getRouteController();
        if (action.equals("start")) {
            controller.startRoute("queueRoute");
        } else if (action.equals("stop")) {
            controller.stopRoute("queueRoute");
        } else {
            throw new IllegalArgumentException("Unknown action: " + action);
        }
        return Response.noContent().build();
    }

    @Path("/queue/consumer")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String receiveMessages() throws Exception {
        final MockEndpoint mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);
        return mockEndpoint.getReceivedExchanges().stream()
                .map(Exchange::getMessage)
                .map(m -> m.getBody(String.class))
                .collect(Collectors.joining("\n"));
    }

    private String componentUri(final QueueOperationDefinition operation) {
        return String.format("azure-storage-queue://%s/%s?operation=%s",
                azureStorageAccountName, QUEUE_NAME,
                operation.name());
    }

    private ExampleMessage createMessage(final QueueMessageItem messageItem) {
        var message = new ExampleMessage();
        var binaryData = messageItem.getBody();
        message.setBody(binaryData == null ? null : binaryData.toString());
        message.setId(messageItem.getMessageId());
        message.setPopReceipt(messageItem.getPopReceipt());
        return message;
    }

}
