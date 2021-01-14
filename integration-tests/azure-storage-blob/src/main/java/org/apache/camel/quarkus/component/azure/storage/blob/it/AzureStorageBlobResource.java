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
package org.apache.camel.quarkus.component.azure.storage.blob.it;

import java.net.URI;
import java.nio.charset.StandardCharsets;

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

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.azure.storage.blob.BlobOperationsDefinition;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/azure-storage-blob")
@ApplicationScoped
public class AzureStorageBlobResource {
    private static final String BLOB_NAME = "test";

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "camel.component.azure-blob.credentials-account-name")
    String azureStorageAccountName;

    @ConfigProperty(name = "camel.component.azure-blob.credentials-account-key")
    String azureStorageAccountKey;

    @ConfigProperty(name = "azure.blob.service.url")
    String azureBlobServiceUrl;

    @ConfigProperty(name = "azure.blob.container.name")
    String azureBlobContainerName;

    @javax.enterprise.inject.Produces
    @Named("azureBlobServiceClient")
    public BlobServiceClient createBlobClient() throws Exception {
        StorageSharedKeyCredential credentials = new StorageSharedKeyCredential(azureStorageAccountName,
                azureStorageAccountKey);
        return new BlobServiceClientBuilder()
                .endpoint(azureBlobServiceUrl)
                .credential(credentials)
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .buildClient();
    }

    @Path("/blob/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createBlob(String message) throws Exception {
        producerTemplate.sendBody(componentUri(BlobOperationsDefinition.uploadBlockBlob),
                message);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/blob/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String readBlob() throws Exception {
        return producerTemplate.requestBodyAndHeader(
                componentUri(BlobOperationsDefinition.getBlob),
                null, Exchange.CHARSET_NAME, StandardCharsets.UTF_8.name(), String.class);
    }

    @Path("/blob/update")
    @PATCH
    @Consumes(MediaType.TEXT_PLAIN)
    public Response updateBlob(String message) throws Exception {
        producerTemplate.sendBody(
                componentUri(BlobOperationsDefinition.uploadBlockBlob),
                message);
        return Response.ok().build();
    }

    @Path("/blob/delete")
    @DELETE
    public Response deleteBlob() throws Exception {
        producerTemplate.sendBody(
                componentUri(BlobOperationsDefinition.deleteBlob),
                null);
        return Response.noContent().build();
    }

    private String componentUri(final BlobOperationsDefinition operation) {
        return String.format("azure-storage-blob://%s/%s?blobServiceClient=#azureBlobServiceClient&operation=%s&blobName=%s",
                azureStorageAccountName, azureBlobContainerName,
                operation.name(), BLOB_NAME);
    }

}
