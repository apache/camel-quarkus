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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.azure.blob.BlobBlock;

@Path("/azure")
@ApplicationScoped
public class AzureBlobResource {

    @Inject
    ProducerTemplate producerTemplate;

    @PostConstruct
    public void init() throws Exception {
        StorageCredentials credentials = StorageCredentials.tryParseCredentials(System.getProperty("azurite.credentials"));
        URI uri = new URI(System.getProperty("azurite.blob.service.url") + "camel-test");
        CloudBlobContainer container = new CloudBlobContainer(uri, credentials);
        container.create();
    }

    @javax.enterprise.inject.Produces
    @Named("azureBlobClient")
    public CloudBlob createBlobClient() throws Exception {
        StorageCredentials credentials = StorageCredentials.tryParseCredentials(System.getProperty("azurite.credentials"));
        URI uri = new URI(System.getProperty("azurite.blob.service.url") + "camel-test/test");
        CloudBlockBlob cloudBlockBlob = new CloudBlockBlob(uri, credentials);
        return cloudBlockBlob;
    }

    @Path("/blob/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createBlob(String message) throws Exception {
        BlobBlock blob = new BlobBlock(new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)));
        producerTemplate.sendBody(
                "azure-blob://devstoreaccount1/camel-test/test?operation=uploadBlobBlocks&azureBlobClient=#azureBlobClient&validateClientURI=false",
                blob);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/blob/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String readBlob() throws Exception {
        return producerTemplate.requestBodyAndHeader(
                "azure-blob://devstoreaccount1/camel-test/test?operation=getBlob&azureBlobClient=#azureBlobClient&validateClientURI=false",
                null, Exchange.CHARSET_NAME, StandardCharsets.UTF_8.name(), String.class);
    }

    @Path("/blob/update")
    @PATCH
    @Consumes(MediaType.TEXT_PLAIN)
    public Response updateBlob(String message) throws Exception {
        producerTemplate.sendBody(
                "azure-blob://devstoreaccount1/camel-test/test?operation=updateBlockBlob&azureBlobClient=#azureBlobClient&validateClientURI=false",
                message);
        return Response.ok().build();
    }

    @Path("/blob/delete")
    @DELETE
    public Response deleteBlob() throws Exception {
        producerTemplate.sendBody(
                "azure-blob://devstoreaccount1/camel-test/test?operation=deleteBlob&azureBlobClient=#azureBlobClient&validateClientURI=false",
                null);
        return Response.noContent().build();
    }

}
