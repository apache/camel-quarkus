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
package org.apache.camel.quarkus.component.azure.it;

import java.net.URI;
import java.util.UUID;

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

import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import io.quarkus.arc.Unremovable;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/azure")
@ApplicationScoped
public class AzureQueueResource {

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
    @Named("azureQueueClient")
    @Unremovable
    public CloudQueue createQueueClient() throws Exception {
        URI uri = new URI(azureQueueServiceUrl + "/" + QUEUE_NAME);
        StorageCredentials credentials = new StorageCredentialsAccountAndKey(azureStorageAccountName, azureStorageAccountKey);
        return new CloudQueue(uri, credentials);
    }

    @Path("/queue/create")
    @POST
    public Response createQueue() throws Exception {
        producerTemplate.sendBody(
                "azure-queue://" + azureStorageAccountName + "/" + QUEUE_NAME
                        + "?operation=createQueue&azureQueueClient=#azureQueueClient&validateClientURI=false",
                null);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/queue/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String retrieveMessage() throws Exception {
        CloudQueueMessage message = producerTemplate.requestBody(
                "azure-queue://" + azureStorageAccountName + "/" + QUEUE_NAME
                        + "?operation=retrieveMessage&azureQueueClient=#azureQueueClient&validateClientURI=false",
                null, CloudQueueMessage.class);
        return message.getMessageContentAsString();
    }

    @Path("/queue/message")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response addMessage(String message) throws Exception {
        producerTemplate.sendBody(
                "azure-queue://" + azureStorageAccountName + "/" + QUEUE_NAME
                        + "?operation=addMessage&azureQueueClient=#azureQueueClient&validateClientURI=false",
                message);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/queue/delete")
    @DELETE
    public Response deleteQueue() throws Exception {
        producerTemplate.sendBody(
                "azure-queue://" + azureStorageAccountName + "/" + QUEUE_NAME
                        + "?operation=deleteQueue&azureQueueClient=#azureQueueClient&validateClientURI=false",
                null);
        return Response.noContent().build();
    }

}
