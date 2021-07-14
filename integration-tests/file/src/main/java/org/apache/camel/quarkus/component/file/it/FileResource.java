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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
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
    public String getFromMock(@PathParam("mockId") String mockId) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:" + mockId, MockEndpoint.class);

        String result = mockEndpoint.getExchanges().stream().map(e -> e.getIn().getBody(String.class))
                .collect(Collectors.joining(SEPARATOR));

        mockEndpoint.reset();

        return result;
    }

    @Path("/create/{folder}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createFile(@PathParam("folder") String folder, byte[] content, @QueryParam("charset") String charset,
            @QueryParam("fileName") String fileName)
            throws Exception {
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

}
