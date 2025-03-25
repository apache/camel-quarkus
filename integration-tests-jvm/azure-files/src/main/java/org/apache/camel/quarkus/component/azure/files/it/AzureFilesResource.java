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
package org.apache.camel.quarkus.component.azure.files.it;

import java.net.URI;

import com.azure.storage.file.share.models.ShareFileItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.file.GenericFile;

@Path("/azure-files")
@ApplicationScoped
public class AzureFilesResource {
    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Path("upload/{fileName}")
    public Response uploadFile(@PathParam("fileName") String fileName, byte[] fileToUpload) throws Exception {
        producerTemplate.sendBodyAndHeader("direct:uploadFile", fileToUpload, Exchange.FILE_NAME, fileName);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @GET
    @Path("downloaded")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response consumeDownloadedFile() {
        GenericFile<ShareFileItem> file = consumerTemplate.receiveBody("seda:downloadedFiles", 10000, GenericFile.class);
        return Response.ok(file.getBody()).build();
    }

    @Path("/route/{routeId}/start")
    @POST
    public void startRoute(@PathParam("routeId") String routeId) throws Exception {
        context.getRouteController().startRoute(routeId);
    }

    @Path("/route/{routeId}/stop")
    @POST
    public void stopRoute(@PathParam("routeId") String routeId) throws Exception {
        context.getRouteController().stopRoute(routeId);
    }
}
