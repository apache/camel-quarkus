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
package org.apache.camel.quarkus.component.dropbox.it;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dropbox.core.v2.files.DownloadErrorException;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;

import static org.apache.camel.component.dropbox.util.DropboxConstants.HEADER_PUT_FILE_NAME;

@Path("/dropbox")
public class DropboxResource {

    public static final String REMOTE_PATH = "/camel/quarkus/";
    public static final String FILE_NAME = "test.txt";
    public static final String FILE_CONTENT = "Hello Camel Quarkus DropBox";

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createFile() throws Exception {
        java.nio.file.Path path = Files.write(Paths.get("target", FILE_NAME), FILE_CONTENT.getBytes(StandardCharsets.UTF_8));
        String result = producerTemplate.requestBodyAndHeader("dropbox://put?" + getCredentialsUriOptions()
                + "&uploadMode=add&localPath=" + path + "&remotePath=" + REMOTE_PATH, null, HEADER_PUT_FILE_NAME, FILE_NAME,
                String.class);
        return Response.created(new URI("https://camel.apache.org/")).entity(result).build();
    }

    @Path("/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response readFile() {
        try {
            String content = producerTemplate.requestBody(
                    "dropbox://get?" + getCredentialsUriOptions() + "&remotePath=" + REMOTE_PATH + FILE_NAME, null,
                    String.class);
            if (content != null) {
                return Response.ok(content).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (CamelExecutionException e) {
            Exception exchangeException = e.getExchange().getException();
            if (exchangeException != null && exchangeException.getCause() instanceof DownloadErrorException) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            throw e;
        }
    }

    @Path("/delete")
    @DELETE
    public Response deleteFile() {
        producerTemplate.requestBody("dropbox://del?" + getCredentialsUriOptions() + "&remotePath=" + REMOTE_PATH + FILE_NAME,
                (Object) null);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private String getCredentialsUriOptions() {
        return "accessToken={{DROPBOX_ACCESS_TOKEN}}" +
                "&clientIdentifier={{DROPBOX_CLIENT_IDENTIFIER}}" +
                "&refreshToken={{DROPBOX_REFRESH_TOKEN}}" +
                "&apiKey={{DROPBOX_API_KEY}}" +
                "&apiSecret={{DROPBOX_API_SECRET}}" +
                "&expireIn={{DROPBOX_ACCESS_TOKEN_EXPIRES_IN}}";
    }
}
