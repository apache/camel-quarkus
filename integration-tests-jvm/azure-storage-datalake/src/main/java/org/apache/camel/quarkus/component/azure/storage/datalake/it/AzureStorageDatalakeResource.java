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
package org.apache.camel.quarkus.component.azure.storage.datalake.it;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
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
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import com.azure.storage.file.datalake.models.FileSystemItem;
import com.azure.storage.file.datalake.models.PathItem;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.azure.storage.datalake.DataLakeConstants;
import org.apache.camel.component.azure.storage.datalake.DataLakeOperationsDefinition;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/azure-storage-datalake")
@ApplicationScoped
public class AzureStorageDatalakeResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = "azure.storage.account-name")
    String azureStorageAccountName;

    @ConfigProperty(name = "azure.storage.account-key")
    String azureStorageAccountKey;

    @ConfigProperty(name = "azure.datalake.service.url")
    String serviceUrl;

    @javax.enterprise.inject.Produces
    @Named("azureDatalakeServiceClient")
    public DataLakeServiceClient createDatalakeServiceClient() throws Exception {
        StorageSharedKeyCredential credentials = new StorageSharedKeyCredential(azureStorageAccountName,
                azureStorageAccountKey);
        return new DataLakeServiceClientBuilder()
                .endpoint(serviceUrl)
                .credential(credentials)
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .buildClient();
    }

    @Path("/filesystem/{filesystem}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createFileSystem(@PathParam("filesystem") String filesystem) throws Exception {
        producerTemplate.sendBody(componentUri(filesystem, DataLakeOperationsDefinition.createFileSystem), null);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/filesystem/{filesystem}/path/{filename}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response upload(@PathParam("filesystem") String filesystem,
            @PathParam("filename") String filename,
            byte[] body) throws Exception {
        producerTemplate.sendBodyAndHeader(
                componentUri(filesystem, DataLakeOperationsDefinition.upload),
                body,
                DataLakeConstants.FILE_NAME,
                filename);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/filesystem/{filesystem}/path/{filename}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFile(@PathParam("filesystem") String filesystem,
            @PathParam("filename") String filename) throws Exception {
        return producerTemplate.requestBodyAndHeader(
                componentUri(filesystem, DataLakeOperationsDefinition.getFile),
                null,
                DataLakeConstants.FILE_NAME,
                filename,
                String.class);
    }

    @Path("/filesystem/{filesystem}/path/{filename}")
    @DELETE
    public void deleteFile(@PathParam("filesystem") String filesystem,
            @PathParam("filename") String filename) throws Exception {
        producerTemplate.sendBodyAndHeader(
                componentUri(filesystem, DataLakeOperationsDefinition.deleteFile),
                null,
                DataLakeConstants.FILE_NAME,
                filename);
    }

    @Path("/filesystem/{filesystem}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public void deleteFileSystem(@PathParam("filesystem") String filesystem) throws Exception {
        producerTemplate.sendBody(componentUri(filesystem, DataLakeOperationsDefinition.deleteFileSystem), null);
    }

    @Path("/filesystem/{filesystem}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listFileSystem(@PathParam("filesystem") String filesystem) throws Exception {
        @SuppressWarnings("unchecked")
        List<FileSystemItem> filesystems = producerTemplate.requestBody(
                componentUri(filesystem, DataLakeOperationsDefinition.listFileSystem),
                null,
                List.class);
        return filesystems.stream()
                .map(FileSystemItem::getName)
                .collect(Collectors.toList());
    }

    @Path("/filesystem/{filesystem}/paths")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listPaths(@PathParam("filesystem") String filesystem) throws Exception {
        @SuppressWarnings("unchecked")
        List<PathItem> filesystems = producerTemplate.requestBody(
                componentUri(filesystem, DataLakeOperationsDefinition.listPaths),
                null,
                List.class);
        return filesystems.stream()
                .map(PathItem::getName)
                .collect(Collectors.toList());
    }

    @Path("/consumer/{filesystem}/path/{filename}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumer(@PathParam("filesystem") String filesystem,
            @PathParam("filename") String filename) throws Exception {
        return consumerTemplate.receiveBody(
                "azure-storage-datalake://" + azureStorageAccountName + "/" + filesystem
                        + "?serviceClient=#azureDatalakeServiceClient&fileName=" + filename,
                10000, String.class);
    }

    private String componentUri(final String filesystem, final DataLakeOperationsDefinition operation) {
        return String.format("azure-storage-datalake://%s%s?serviceClient=#azureDatalakeServiceClient&operation=%s",
                azureStorageAccountName,
                filesystem == null ? "" : ("/" + filesystem),
                operation.name());
    }

}
