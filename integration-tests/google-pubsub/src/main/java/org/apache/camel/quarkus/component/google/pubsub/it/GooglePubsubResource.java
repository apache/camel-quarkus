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
package org.apache.camel.quarkus.component.google.pubsub.it;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.protobuf.Timestamp;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.google.pubsub.GooglePubsubConstants;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/google-pubsub")
public class GooglePubsubResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Inject
    GooglePubSubRoutes.AcKFailing acKFailing;

    @POST
    public Response sendStringToTopic(String message) {
        producerTemplate.sendBody("google-pubsub:{{project.id}}:{{google-pubsub.topic-name}}", message);
        return Response.created(URI.create("https://camel.apache.org")).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response consumeStringFromTopic() {
        Exchange exchange = consumerTemplate
                .receive("google-pubsub:{{project.id}}:{{google-pubsub.subscription-name}}?synchronousPull=true", 5000L);
        //convert timestamp to long to avoid serializiong problems
        Map<String, Object> retVal = new HashMap<>();
        retVal.put("body", exchange.getIn().getBody(String.class));
        retVal.putAll(exchange.getIn().getHeaders().entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().replaceFirst("\\.", "_"),
                e -> {
                    if (GooglePubsubConstants.PUBLISH_TIME.equals(e.getKey()) && e.getValue() instanceof Timestamp) {
                        return ((Timestamp) e.getValue()).getSeconds() * 1000;
                    }
                    return e.getValue();
                })));
        return Response.ok(retVal).build();
    }

    @Path("/pojo")
    @POST
    public Response sendPojoToTopic(String fruitName) {
        Fruit fruit = new Fruit(fruitName);
        producerTemplate.sendBody("google-pubsub:{{project.id}}:{{google-pubsub.topic-name}}", fruit);
        return Response.created(URI.create("https://camel.apache.org")).build();
    }

    @Path("/pojo")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response consumePojoFromTopic() {
        Object response = consumerTemplate
                .receiveBody("google-pubsub:{{project.id}}:{{google-pubsub.subscription-name}}?synchronousPull=true", 5000L);
        return Response.ok(response).build();
    }

    @Path("/sendToEndpoint")
    @POST
    public Response sentToEndpoint(String message,
            @QueryParam("toEndpoint") String toEndpoint)
            throws Exception {
        producerTemplate.sendBody(toEndpoint, message);
        return Response.created(URI.create("https://camel.apache.org")).build();
    }

    @Path("/getFromEndpoint")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFromEndpoint(@QueryParam("fromEndpoint") String fromEndpoint) throws Exception {
        return consumerTemplate.receiveBody(fromEndpoint, 5000, String.class);
    }

    @Path("receive/subscription/{subscriptionName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveFromSubscription(@PathParam("subscriptionName") String subscriptionName) throws Exception {
        return consumeEndpoint(subscriptionName, null);
    }

    @Path("receive/subscriptionOrdering/{subscriptionName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveFromSubscriptionOrdered(@PathParam("subscriptionName") String subscriptionName) throws Exception {

        return consumeEndpoint(subscriptionName,
                "&messageOrderingEnabled=true&pubsubEndpoint=pubsub.googleapis.com:443");
    }

    private String consumeEndpoint(String subscriptionName, String parameters) {
        String url = "google-pubsub:{{project.id}}:{{" + subscriptionName + "}}?synchronousPull=true";
        if (parameters != null && !"".equals(parameters)) {
            url = url + parameters;
        }
        Exchange ex = consumerTemplate.receive(url, 5000);

        if (ex != null) {
            return ex.getIn().getBody(String.class);
        }

        return null;
    }

    @Path("receive/mock/{mockName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String mockReceive(@PathParam("mockName") String mockName)
            throws Exception {
        MockEndpoint mock = context.getEndpoint(mockName, MockEndpoint.class);

        List<String> results = mock.getExchanges().stream().map(e -> e.getIn().getBody(String.class))
                .collect(Collectors.toList());
        String s = results.stream().collect(Collectors.joining(","));
        return s;
    }

    @Path("setFail/")
    @POST
    public Response setFail(boolean fail)
            throws Exception {
        acKFailing.setFail(fail);
        return Response.created(URI.create("https://camel.apache.org")).build();
    }

    @Path("/stopConsumer")
    @GET
    public void stopConsumer() throws Exception {
        consumerTemplate.stop();
    }

    @Path("/resetMock/{mockName}")
    @GET
    public Response resetMock(@PathParam("mockName") String mockName) {
        MockEndpoint mock = context.getEndpoint(mockName, MockEndpoint.class);
        mock.reset();
        return Response.created(URI.create("https://camel.apache.org")).build();
    }
}
