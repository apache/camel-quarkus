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
package org.apache.camel.quarkus.component.box.it;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.box.sdk.BoxFile;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/box")
@ApplicationScoped
public class BoxResource {

    private static final Logger LOG = Logger.getLogger(BoxResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/uploadFile/{parentFolder}/{name}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadFile(String content, @PathParam("parentFolder") String parentFolder, @PathParam("name") String name)
            throws Exception {
        LOG.infof("Uploading file to box: %s under the root folder with name %s", content, name);
        final Map<String, Object> headers = new HashMap<>();
        headers.put("CamelBox.parentFolderId", parentFolder);
        headers.put("CamelBox.content", new ByteArrayInputStream(content.getBytes()));
        headers.put("CamelBox.fileName", name);
        final BoxFile response = producerTemplate.requestBodyAndHeaders("direct:upload-file", null, headers, BoxFile.class);
        LOG.infof("Got response from box: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getID())
                .build();
    }

    @Path("/downloadFile")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response downloadFile(String fileId) throws Exception {
        LOG.infof("Downloading from box: %s", fileId);
        final Map<String, Object> headers = new HashMap<>();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        headers.put("CamelBox.output", output);
        producerTemplate.requestBodyAndHeaders("direct:download-file", fileId, headers, OutputStream.class);
        String response = output.toString();
        LOG.infof("Got response from box: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/deleteFile")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteFile(String fileId) throws Exception {
        LOG.infof("Deleting file from Box with id: %s", fileId);
        final String response = producerTemplate.requestBody("direct:delete-file", fileId, String.class);
        LOG.infof("Got response from box: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }
}
