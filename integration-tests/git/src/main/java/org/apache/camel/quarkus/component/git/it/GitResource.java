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
package org.apache.camel.quarkus.component.git.it;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/git")
@ApplicationScoped
public class GitResource {

    private static final Logger LOG = Logger.getLogger(GitResource.class);
    private static final String repoName = "testRepo";
    private static final String fileName = "test.java";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/commits")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String commits() throws Exception {
        final String message = consumerTemplate.receiveBody("git:target/" + repoName + "?type=commit",
                String.class);
        LOG.infof("commits Received from git: %s", message);
        return message;
    }

    @Path("/branches")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String branches() throws Exception {
        final String message = consumerTemplate.receiveBody("git:target/" + repoName + "?type=branch",
                String.class);
        LOG.infof("branches Received from git: %s", message);
        return message;
    }

    @Path("/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(String message) throws Exception {
        LOG.infof("Sending to git: %s", message);
        Exchange initResponse = producerTemplate.request("git:target/" + repoName + "?operation=init",
                exchange -> exchange.getIn());

        // We're gonna put a real file in here
        File newFile = new File("target/" + repoName + "/" + fileName);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
            writer.write("this is a fake java file");
            writer.close();
        } catch (IOException ioe) {
            throw ioe;
        }

        Object addResponse = producerTemplate.requestBodyAndHeader("git:target/" + repoName + "?operation=add", "foobar",
                "CamelGitFilename", "test.java");

        Exchange commitResponse = producerTemplate.request("git:target/" + repoName + "?operation=commit",
                exchange -> exchange.getIn().setHeader("CamelGitCommitMessage", "firstcommit"));

        LOG.infof("Got response from git: %s", commitResponse);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(commitResponse.getMessage().getHeader("CamelGitCommitId"))
                .build();
    }
}
