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
package org.apache.camel.quarkus.component.file.it;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.awaitility.Awaitility;

@Path("/file")
@ApplicationScoped
public class FileResource {

    public static String CONSUME_BATCH = "consumeBatch";
    public static String SORT_BY = "sortBy";
    public static String SEPARATOR = ";";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Path("/get/{folder}/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFile(@PathParam("folder") String folder, @PathParam("name") String name) throws Exception {
        StringBuilder url = new StringBuilder(String.format("file:target/%s?fileName=%s", folder, name));
        String s = consumerTemplate.receiveBodyNoWait(url.toString(), String.class);

        return s;
    }

    @Path("/getBatch")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getBatch() throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:" + CONSUME_BATCH, MockEndpoint.class);

        context.getRouteController().startRoute(CONSUME_BATCH);

        Map<String, Object> result = new HashMap<>();

        mockEndpoint.getExchanges().stream().forEach(
                e -> result.put(e.getIn().getBody(String.class), e.getProperty(Exchange.BATCH_INDEX)));

        return result;
    }

    @Path("/startRoute")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void startRoute(String routeId) throws Exception {
        context.getRouteController().startRoute(routeId);
    }

    @Path("/getFromMock/{mockId}")
    @GET
    public String getFromMock(@PathParam("mockId") String mockId) {
        System.out.println("CAMEL-QUARKUS-3584 => FileResource.getFromMock(" + mockId + ").thread.id => "
                + Thread.currentThread().getId());
        MockEndpoint mockEndpoint = context.getEndpoint("mock:" + mockId, MockEndpoint.class);
        System.out.println("CAMEL-QUARKUS-3584 => FileResource.getFromMock(" + mockId + ").mockEndpoint => 0x"
                + System.identityHashCode(mockEndpoint));

        String result = mockEndpoint.getExchanges().stream().map(e -> e.getIn().getBody(String.class))
                .collect(Collectors.joining(SEPARATOR));

        System.out.println("CAMEL-QUARKUS-3584 => FileResource.getFromMock(" + mockId + ") returns => " + result);

        return result;
    }

    @Path("/resetMock/{mockId}")
    @GET
    public void resetMock(@PathParam("mockId") String mockId) {
        System.out.println("CAMEL-QUARKUS-3584 => FileResource.resetMock().thread.id => " + Thread.currentThread().getId());
        MockEndpoint mockEndpoint = context.getEndpoint("mock:" + mockId, MockEndpoint.class);
        System.out.println(
                "CAMEL-QUARKUS-3584 => FileResource.resetMock.mockEndpoint => 0x" + System.identityHashCode(mockEndpoint));
        mockEndpoint.reset();
    }

    @Path("/create/{folder}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createFile(@PathParam("folder") String folder, byte[] content, @QueryParam("charset") String charset,
            @QueryParam("fileName") String fileName)
            throws Exception {
        System.out.println("CAMEL-QUARKUS-3584 => FileResource.createFile().thread.id => " + Thread.currentThread().getId());
        StringBuilder url = new StringBuilder("file:target/" + folder + "?initialDelay=10");
        if (charset != null && !charset.equals("")) {
            url.append("&charset=").append(charset);
        }
        Exchange response = producerTemplate.request(url.toString(),
                exchange -> {
                    exchange.getIn().setBody(content);
                    if (fileName != null && !fileName.equals("")) {
                        exchange.getIn().setHeader(Exchange.FILE_NAME, fileName);
                    }
                });
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getMessage().getHeader(Exchange.FILE_NAME_PRODUCED))
                .build();
    }

    @Path("/route/{route}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public String pollEnrich(String body, @PathParam("route") String route) throws Exception {
        return producerTemplate.requestBody("direct:" + route, body, String.class);
    }

    @Path("/writeThenReadFileWithCharsetShouldSucceed")
    @GET
    public void writeThenReadFileWithCharsetShouldSucceed() throws Exception {

        // Delete any charset encoded files that would reside from a previous run
        Files.deleteIfExists(Paths.get("target/charsetIsoRead/charsetEncodedFile.txt"));
        Files.deleteIfExists(Paths.get("target/charsetIsoWrite/charsetEncodedFile.txt"));

        // Using a charset that has few chance to be the default one on the build platform
        String charsetName = "ISO-8859-1";
        String unencodedContent = "A string with ð char";
        byte[] encodedContent = unencodedContent.getBytes(charsetName);

        // Produce in the folder named 'charsetIsoWrite' and check the content is encoded as expected
        producerTemplate.request("file:target/charsetIsoWrite/?charset=" + charsetName, ex -> {
            ex.getMessage().setHeader(Exchange.FILE_NAME, "charsetEncodedFile.txt");
            ex.getMessage().setBody(unencodedContent);
        });
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            File file = new File("target/charsetIsoWrite/charsetEncodedFile.txt");
            return Arrays.equals(encodedContent, Files.readAllBytes(file.toPath()));
        });

        // Move the encoded file to the read folder
        java.nio.file.Path source = Paths.get("target/charsetIsoWrite/charsetEncodedFile.txt");
        java.nio.file.Path destination = Paths.get("target/charsetIsoRead/charsetEncodedFile.txt");
        Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);

        // Start the route to consume the encoded file
        context.getRouteController().startRoute("charsetIsoRead");

        // Check that the consumed file content has been decoded as expected
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            String decodedContent = getFromMock("charsetIsoRead");
            return unencodedContent.equals(decodedContent);
        });
    }

}
