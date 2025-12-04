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
package org.apache.camel.quarkus.component.ibm.cos.it;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import com.ibm.cloud.objectstorage.services.s3.model.S3ObjectSummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.ibm.cos.IBMCOSConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.RouteController;

@Path("/ibm-cos")
@ApplicationScoped
public class IBMCloudObjectStorageResource {

    @Inject
    ProducerTemplate producerTemplate;
    @Inject
    CamelContext context;

    @Path("/bucket/create")
    @POST
    public Response createBucket(String content) throws URISyntaxException {
        Exchange exchange = producerTemplate.request("direct:create-bucket", new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setBody(content);
            }
        });

        return responseFrom(exchange);
    }

    @Path("/bucket/delete")
    @POST
    public Response deleteBucket(String content) throws URISyntaxException {
        Exchange exchange = producerTemplate.request("direct:delete-bucket", new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setBody(content);
            }
        });

        return responseFrom(exchange);
    }

    @Path("/object/put")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createObject(String content) throws URISyntaxException {
        Exchange exchange = producerTemplate.request("direct:put-object", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setBody(content);
            }
        });

        return responseFrom(exchange);
    }

    @Path("/object/delete")
    @POST
    public Response deleteObject() throws URISyntaxException {
        Exchange exchange = producerTemplate.request("direct:delete-object", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                // no action
            }
        });

        return responseFrom(exchange);
    }

    @SuppressWarnings("unchecked")
    @Path("/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject list() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        List<S3ObjectSummary> objects = producerTemplate.requestBody("direct:list", null, List.class);
        objects.stream()
                .map(blobItem -> objectBuilder.add("key", blobItem.getKey()))
                .forEach(arrayBuilder::add);

        objectBuilder.add("objects", arrayBuilder);

        return objectBuilder.build();
    }

    @Path("/object/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String readObject() {
        return producerTemplate.requestBody("direct:read", null, String.class);
    }

    @Path("/consumer/{action}")
    @POST
    public Response modifyConsumerRouteState(@PathParam("action") String action) throws Exception {
        RouteController controller = context.getRouteController();
        if (action.equals("start")) {
            controller.startRoute(IBMCloudObjectStorageRoutes.CONSUME_ROUTE_ID);
        } else if (action.equals("stop")) {
            controller.stopRoute(IBMCloudObjectStorageRoutes.CONSUME_ROUTE_ID);
        } else {
            throw new IllegalArgumentException("Unknown action: " + action);
        }
        return Response.noContent().build();
    }

    @Path("/consumer")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String receiveMessages() {
        final MockEndpoint mockEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);
        return mockEndpoint.getReceivedExchanges().stream()
                .map(Exchange::getMessage)
                .map(m -> m.getBody(String.class))
                .collect(Collectors.joining("\n"));
    }

    public Response responseFrom(Exchange exchange) throws URISyntaxException {
        if (!exchange.isFailed()) {
            Message message = exchange.getMessage();
            return Response.created(new URI("https://camel.apache.org/"))
                    .entity(message.getHeader(IBMCOSConstants.E_TAG))
                    .build();
        }

        return Response.serverError().build();
    }

}
