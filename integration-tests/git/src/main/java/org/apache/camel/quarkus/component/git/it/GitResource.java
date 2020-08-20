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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.git.GitConstants;
import org.eclipse.jgit.revwalk.RevCommit;

@Path("/git")
@ApplicationScoped
public class GitResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/init/{repoName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response init(@PathParam("repoName") String repoName) throws Exception {
        producerTemplate.requestBody("git:target/" + repoName + "?operation=init", (Object) null);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity("target/" + repoName)
                .build();

    }

    @Path("/add-and-commit/{repoName}/{file:(.+)?}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addAndCommit(@PathParam("repoName") String repoName, @PathParam("file") String file, byte[] content)
            throws Exception {
        final java.nio.file.Path path = Paths.get("target/" + repoName + "/" + file);
        Files.createDirectories(path.getParent());
        Files.write(path, content);
        producerTemplate.requestBodyAndHeader("git:target/" + repoName + "?operation=add", (Object) null,
                GitConstants.GIT_FILE_NAME,
                file);
        producerTemplate.requestBodyAndHeader("git:target/" + repoName + "?operation=commit", (Object) null,
                GitConstants.GIT_COMMIT_MESSAGE, "Add " + file);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity("target/" + repoName + "/" + file)
                .build();

    }

    @Path("/log/{repoName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String log(@PathParam("repoName") String repoName) throws Exception {
        Iterable<RevCommit> it = producerTemplate.requestBody("git:target/" + repoName + "?operation=log", null,
                Iterable.class);
        return StreamSupport.stream(it.spliterator(), false)
                .map(commit -> commit.getName() + " " + commit.getFullMessage())
                .collect(Collectors.joining("\n"));
    }

}
