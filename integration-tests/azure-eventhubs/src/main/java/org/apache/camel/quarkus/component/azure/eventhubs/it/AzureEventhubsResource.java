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

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/azure-eventhubs")
@ApplicationScoped
public class AzureEventhubsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = "azure.storage.account-name")
    String azureStorageAccountName;

    @ConfigProperty(name = "azure.storage.account-key")
    String azureStorageAccountKey;

    @ConfigProperty(name = "azure.event.hubs.connection.string")
    String connectionString;

    @ConfigProperty(name = "azure.blob.container.name")
    String azureBlobContainerName;

    @Path("/receive-events")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveEvents() throws Exception {

        final String endpointUri = "azure-eventhubs:?connectionString=RAW(" + connectionString
                + ")&blobAccountName=RAW(" + azureStorageAccountName
                + ")&blobAccessKey=RAW(" + azureStorageAccountKey
                + ")&blobContainerName=RAW(" + azureBlobContainerName + ")";
        return consumerTemplate.receiveBody(endpointUri,
                10000L,
                String.class);
    }

    @Path("/send-events")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response sendEvents(String body) throws Exception {

        final String endpointUri = "azure-eventhubs:?connectionString=RAW(" + connectionString + ")";

        producerTemplate.sendBody(endpointUri, body);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

}
