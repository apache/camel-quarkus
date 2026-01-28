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
package org.apache.camel.quarkus.component.docling.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.docling.DocumentMetadata;
import org.jboss.logging.Logger;

@Path("/docling")
@ApplicationScoped
@RegisterForReflection(targets = DocumentMetadata.class, methods = true)
public class DoclingResource {

    private static final Logger LOG = Logger.getLogger(DoclingResource.class);
    private static final String COMPONENT_DOCLING = "docling";

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/convert/markdown")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response convertToMarkdown(String documentContent) throws IOException {
        return convert(documentContent, "direct:convertToMarkdown", "Failed to convert to markdown");
    }

    @Path("/convert/html")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_HTML)
    public Response convertToHtml(String documentContent) throws IOException {
        return convert(documentContent, "direct:convertToHtml", "Failed to convert to HTML");
    }

    @Path("/extract/text")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response extractText(String documentContent) throws IOException {
        java.nio.file.Path tempFile = Files.createTempFile("docling-test", ".md");
        Files.writeString(tempFile, documentContent);
        try {
            String result = producerTemplate.requestBody("direct:extractText", tempFile.toString(), String.class);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.error("Failed to extract text", e);
            return Response.status(500).entity("Error: " + e.getMessage()).build();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Path("/test/resource")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response testResourceDocument(@QueryParam("name") String resourceName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                return Response.status(404).entity("Resource not found: " + resourceName).build();
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Response.ok(content).build();
        } catch (Exception e) {
            LOG.error("Failed to read resource", e);
            return Response.status(500).entity("Error: " + e.getMessage()).build();
        }
    }

    @Path("/component/available")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response isComponentAvailable() {
        boolean available = context.getComponent(COMPONENT_DOCLING) != null;
        return Response.ok(String.valueOf(available)).build();
    }

    @Path("/convert/json")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response convertToJson(String documentContent) throws IOException {
        return convert(documentContent, "direct:convertToJson", "Failed to convert to JSON");
    }

    @Path("/async/convert/markdown")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response convertToMarkdownAsync(String documentContent) throws IOException {
        return convert(documentContent, "direct:convertToMarkdownAsync", "Failed to convert to markdown async");
    }

    @Path("/async/convert/html")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_HTML)
    public Response convertToHtmlAsync(String documentContent) throws IOException {
        return convert(documentContent, "direct:convertToHtmlAsync", "Failed to convert to HTML async");
    }

    @Path("/async/convert/json")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response convertToJsonAsync(String documentContent) throws IOException {
        return convert(documentContent, "direct:convertToJsonAsync", "Failed to convert to JSON async");
    }

    @Path("/metadata/extract")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public DocumentMetadata extractMetadata(String documentContent) throws IOException {
        java.nio.file.Path tempFile = Files.createTempFile("docling-test", ".md");
        Files.writeString(tempFile, documentContent);
        try {
            return producerTemplate.requestBody("direct:extractMetadata", tempFile.toString(),
                    DocumentMetadata.class);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Path("/metadata/extract/pdf")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public DocumentMetadata extractMetadataFromPdf() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("multi_page.pdf")) {
            java.nio.file.Path tempFile = Files.createTempFile("docling-test-multi_page", ".pdf");
            Files.copy(is, tempFile.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
            try {
                return producerTemplate.requestBody("direct:extractMetadata", tempFile.toString(),
                        DocumentMetadata.class);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    @Path("/convert/json/cli")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response convertToJsonWithCLI(String documentContent) throws IOException {
        return convert(documentContent, "direct:convertToJsonWithCLI", "Failed to convert to JSON with CLI");
    }

    private Response convert(String documentContent, String endpointUri, String logErrorMessage) throws IOException {
        java.nio.file.Path tempFile = Files.createTempFile("docling-test", ".md");
        Files.writeString(tempFile, documentContent);
        try {
            String result = producerTemplate.requestBody(endpointUri, tempFile.toString(), String.class);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.error(logErrorMessage, e);
            return Response.status(500).entity("Error: " + e.getMessage()).build();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

}
