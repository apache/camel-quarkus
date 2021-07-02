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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.util.BinaryData;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import io.quarkus.arc.Unremovable;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.azure.storage.queue.QueueOperationDefinition;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/azure-storage-queue")
@ApplicationScoped
public class AzureStorageQueueResource {

    private static final String QUEUE_NAME = "camel-quarkus-" + UUID.randomUUID().toString();

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "azure.storage.account-name")
    String azureStorageAccountName;

    @ConfigProperty(name = "azure.storage.account-key")
    String azureStorageAccountKey;

    @ConfigProperty(name = "azure.queue.service.url")
    String azureQueueServiceUrl;

    @javax.enterprise.inject.Produces
    @Named("azureQueueServiceClient")
    @Unremovable
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
    @Produces(MediaType.TEXT_PLAIN)
    public String retrieveMessage() throws Exception {
        @SuppressWarnings("unchecked")
        List<QueueMessageItem> messages = producerTemplate.requestBody(
                componentUri(QueueOperationDefinition.receiveMessages),
                null, List.class);
        return messages.stream()
                .map(QueueMessageItem::getBody)
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

    @Path("/queue/delete")
    @DELETE
    public Response deleteQueue() throws Exception {
        producerTemplate.sendBody(
                componentUri(QueueOperationDefinition.deleteQueue),
                null);
        return Response.noContent().build();
    }

    private String componentUri(final QueueOperationDefinition operation) {
        return String.format("azure-storage-queue://%s/%s?operation=%s",
                azureStorageAccountName, QUEUE_NAME,
                operation.name());
    }

}
