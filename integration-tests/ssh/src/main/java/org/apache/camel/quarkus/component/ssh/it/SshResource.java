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
package org.apache.camel.quarkus.component.ssh.it;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/ssh")
@ApplicationScoped
public class SshResource {

    private final String user = "test";
    private final String password = "password";

    @ConfigProperty(name = "quarkus.ssh.host")
    private String host;
    @ConfigProperty(name = "quarkus.ssh.port")
    private String port;

    @Inject
    ProducerTemplate producerTemplate;

    @POST
    @Path("/file/{fileName}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response writeToFile(@PathParam("fileName") String fileName, String content)
            throws URISyntaxException {

        String sshWriteFileCommand = String.format("printf \"%s\" > %s", content, fileName);
        producerTemplate.sendBody(
                String.format("ssh:%s:%s?username=%s&password=%s", host, port, user, password),
                sshWriteFileCommand);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @GET
    @Path("/file/{fileName}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response readFile(@PathParam("fileName") String fileName) throws URISyntaxException {

        String sshReadFileCommand = String.format("cat %s", fileName);
        String content = producerTemplate.requestBody(
                String.format("ssh:%s:%s?username=%s&password=%s", host, port, user, password),
                sshReadFileCommand,
                String.class);

        return Response
                .ok(content)
                .build();
    }
}
