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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.microsoft.azure.storage.queue.CloudQueueMessage;
import org.apache.camel.ProducerTemplate;

@Path("/azure")
@ApplicationScoped
public class AzureQueueResource {

    private static final String QUEUE_NAME = UUID.randomUUID().toString();

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/queue/create")
    @POST
    public Response createQueue() throws Exception {
        producerTemplate.sendBody("azure-queue://{{env:AZURE_STORAGE_ACCOUNT}}/" + QUEUE_NAME + "?operation=createQueue", null);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/queue/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String retrieveMessage() throws Exception {
        CloudQueueMessage message = producerTemplate.requestBody(
                "azure-queue://{{env:AZURE_STORAGE_ACCOUNT}}/" + QUEUE_NAME + "?operation=retrieveMessage",
                null, CloudQueueMessage.class);
        return message.getMessageContentAsString();
    }

    @Path("/queue/message")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response addMessage(String message) throws Exception {
        producerTemplate.sendBody("azure-queue://{{env:AZURE_STORAGE_ACCOUNT}}/" + QUEUE_NAME + "?operation=addMessage",
                message);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/queue/delete")
    @DELETE
    public Response deleteQueue() throws Exception {
        producerTemplate.sendBody("azure-queue://{{env:AZURE_STORAGE_ACCOUNT}}/" + QUEUE_NAME + "?operation=deleteQueue",
                null);
        return Response.noContent().build();
    }

}
