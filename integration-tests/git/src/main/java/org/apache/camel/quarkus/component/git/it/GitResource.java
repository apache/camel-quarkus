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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
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
import org.apache.camel.component.git.GitConstants;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.SystemReader;

@Path("/git")
@ApplicationScoped
public class GitResource {

    @Inject
    ProducerTemplate producerTemplate;

    public void init(@Observes StartupEvent startupEvent) throws IOException, ConfigInvalidException {
        // Avoid consuming any local git config that may affect the tests
        SystemReader.getInstance().getUserConfig().clear();
    }

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
