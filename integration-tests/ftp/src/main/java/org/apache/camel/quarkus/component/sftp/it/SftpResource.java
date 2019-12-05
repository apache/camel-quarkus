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
package org.apache.camel.quarkus.component.sftp.it;

import java.net.URI;
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

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;

@Path("/sftp")
@ApplicationScoped
public class SftpResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/get/{fileName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFile(@PathParam("fileName") String fileName) throws Exception {
        return consumerTemplate.receiveBodyNoWait(
                "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?password=admin&localWorkDirectory=target&fileName="
                        + fileName,
                String.class);
    }

    @Path("/create/{fileName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createFile(@PathParam("fileName") String fileName, String fileContent)
            throws Exception {
        producerTemplate.sendBodyAndHeader("sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?password=admin", fileContent,
                Exchange.FILE_NAME, fileName);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }
}
