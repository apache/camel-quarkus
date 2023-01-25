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
package org.apache.camel.quarkus.component.ftps.it;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;

@Path("/ftps")
@ApplicationScoped
public class FtpsResource {

    private static final long TIMEOUT_MS = 1000;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/get/{fileName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFile(@PathParam("fileName") String fileName) {
        return consumerTemplate.receiveBody(
                "ftps://admin@localhost:{{camel.ftps.test-port}}/ftps?password=admin&fileName=" + fileName,
                TIMEOUT_MS,
                String.class);
    }

    @Path("/create/{fileName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createFile(@PathParam("fileName") String fileName, String fileContent)
            throws Exception {
        producerTemplate.sendBodyAndHeader("ftps://admin@localhost:{{camel.ftps.test-port}}/ftps?password=admin", fileContent,
                Exchange.FILE_NAME, fileName);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/delete/{fileName}")
    @DELETE
    public void deleteFile(@PathParam("fileName") String fileName) {
        consumerTemplate.receiveBody(
                "ftps://admin@localhost:{{camel.ftps.test-port}}/ftps?password=admin&delete=true&fileName=" + fileName,
                TIMEOUT_MS,
                String.class);
    }

    @Path("/moveToDoneFile/{fileName}")
    @PUT
    public void moveToDoneFile(@PathParam("fileName") String fileName) {
        consumerTemplate.receiveBody(
                "ftps://admin@localhost:{{camel.ftps.test-port}}/ftps?password=admin&move=${headers.CamelFileName}.done&fileName="
                        + fileName,
                TIMEOUT_MS,
                String.class);
    }
}
