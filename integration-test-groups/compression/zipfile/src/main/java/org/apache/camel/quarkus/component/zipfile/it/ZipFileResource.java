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
package org.apache.camel.quarkus.component.zipfile.it;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Path("/zipfile")
@ApplicationScoped
public class ZipFileResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext camelContext;

    @Path("/compress")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response compress(byte[] uncompressedMessage) throws Exception {

        final String endopintUri = "direct:zipfile-compress";
        final byte[] compressedMessage = producerTemplate.requestBody(endopintUri, uncompressedMessage, byte[].class);

        return Response.created(new URI("https://camel.apache.org/"))
                .header("content-length", compressedMessage.length)
                .entity(compressedMessage)
                .build();
    }

    @Path("/uncompress")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response uncompress(byte[] compressedMessage) throws Exception {

        final String endpointUri = "direct:zipfile-uncompress";
        final byte[] uncompressMessage = producerTemplate.requestBody(endpointUri, compressedMessage, byte[].class);

        return Response.created(new URI("https://camel.apache.org/"))
                .header("content-length", uncompressMessage.length)
                .entity(uncompressMessage)
                .build();
    }

    @Path("/splitIteratorShouldSucceed")
    @GET
    public void splitIteratorShouldSucceed() throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(baos)) {
                zipOutputStream.putNextEntry(new ZipEntry("first"));
                zipOutputStream.write("first-entry".getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
                zipOutputStream.putNextEntry(new ZipEntry("second"));
                zipOutputStream.write("second-entry".getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
                zipOutputStream.close();
            }

            MockEndpoint mockSplitIterator = camelContext.getEndpoint("mock:zipfile-splititerator", MockEndpoint.class);
            mockSplitIterator.expectedBodiesReceived("first-entry", "second-entry");
            producerTemplate.requestBody("direct:zipfile-splititerator", baos.toByteArray());
            mockSplitIterator.assertIsSatisfied();
        }
    }

    @Path("/aggregateShouldSucceed")
    @GET
    public void aggregateShouldSucceed() throws Exception {

        File firstInputFile = null, secondInputFile = null;

        try {
            firstInputFile = File.createTempFile("cq-zipfile-it-", "-first");
            Files.write(firstInputFile.toPath(), Arrays.asList("first-content"), StandardCharsets.UTF_8);
            secondInputFile = File.createTempFile("cq-zipfile-it-", "-second");
            Files.write(secondInputFile.toPath(), Arrays.asList("second-content"), StandardCharsets.UTF_8);

            MockEndpoint mockAggregate = camelContext.getEndpoint("mock:zipfile-aggregate", MockEndpoint.class);
            mockAggregate.expectedMessageCount(1);
            producerTemplate.requestBody("direct:zipfile-aggregate", firstInputFile);
            producerTemplate.requestBody("direct:zipfile-aggregate", secondInputFile);
            mockAggregate.assertIsSatisfied();

            assertNotNull(mockAggregate.getReceivedExchanges());
            assertEquals(1, mockAggregate.getReceivedExchanges().size());
            Message producedMessage = mockAggregate.getReceivedExchanges().get(0).getMessage();
            assertNotNull(producedMessage);
            byte[] producedZip = producedMessage.getBody(byte[].class);

            try (ByteArrayInputStream producedZipBytes = new ByteArrayInputStream(producedZip)) {
                try (ZipInputStream producedZipInputStream = new ZipInputStream(producedZipBytes)) {
                    ZipEntry firstProducedEntry = producedZipInputStream.getNextEntry();
                    assertNotNull(firstProducedEntry);
                    assertEquals(firstInputFile.getName(), firstProducedEntry.getName());
                    ZipEntry secondProducedEntry = producedZipInputStream.getNextEntry();
                    assertNotNull(secondProducedEntry);
                    assertEquals(secondInputFile.getName(), secondProducedEntry.getName());
                }
            }
        } finally {
            if (firstInputFile != null) {
                firstInputFile.delete();
            }
            if (secondInputFile != null) {
                secondInputFile.delete();
            }
        }
    }
}
