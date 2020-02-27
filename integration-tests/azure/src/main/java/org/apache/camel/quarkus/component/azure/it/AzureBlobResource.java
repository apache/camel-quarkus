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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.azure.blob.BlobBlock;

@Path("/azure")
@ApplicationScoped
public class AzureBlobResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/blob/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createBlob(String message) throws Exception {
        BlobBlock blob = new BlobBlock(new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)));
        producerTemplate.sendBody("azure-blob://{{env:AZURE_STORAGE_ACCOUNT}}/camel-test/test?operation=uploadBlobBlocks",
                blob);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/blob/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String readBlob() throws Exception {
        return producerTemplate.requestBodyAndHeader(
                "azure-blob://{{env:AZURE_STORAGE_ACCOUNT}}/camel-test/test?operation=getBlob",
                null, Exchange.CHARSET_NAME, StandardCharsets.UTF_8.name(), String.class);
    }

    @Path("/blob/update")
    @PATCH
    @Consumes(MediaType.TEXT_PLAIN)
    public Response updateBlob(String message) throws Exception {
        producerTemplate.sendBody("azure-blob://{{env:AZURE_STORAGE_ACCOUNT}}/camel-test/test?operation=updateBlockBlob",
                message);
        return Response.ok().build();
    }

    @Path("/blob/delete")
    @DELETE
    public Response deleteBlob() throws Exception {
        producerTemplate.sendBody("azure-blob://{{env:AZURE_STORAGE_ACCOUNT}}/camel-test/test?operation=deleteBlob",
                null);
        return Response.noContent().build();
    }

}
