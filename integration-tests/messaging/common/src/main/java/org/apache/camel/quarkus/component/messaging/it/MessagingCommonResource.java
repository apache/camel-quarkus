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
package org.apache.camel.quarkus.component.messaging.it;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.inject.Inject;
import jakarta.jms.BytesMessage;
import jakarta.jms.MapMessage;
import jakarta.jms.TextMessage;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.messaging.it.util.resolver.JmsMessageResolver;
import org.apache.camel.quarkus.component.messaging.it.util.scheme.ComponentScheme;

@Path("/messaging")
public class MessagingCommonResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Inject
    JmsMessageResolver messageResolver;

    @Inject
    ComponentScheme componentScheme;

    @Inject
    MessagingPojoConsumer pojoConsumer;

    @Path("/{queueName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumeJmsQueueMessage(@PathParam("queueName") String queueName) {
        return consumerTemplate.receiveBody(componentScheme + ":queue:" + queueName, 5000,
                String.class);
    }

    @Path("/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response produceJmsQueueMessage(@PathParam("queueName") String queueName, String message) throws Exception {
        producerTemplate.sendBody(componentScheme + ":queue:" + queueName, message);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("{queueName}/type/{type}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response jmsMessageType(@PathParam("queueName") String queueName, @PathParam("type") String type, String messageBody)
            throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:jmsType", MockEndpoint.class);
        mockEndpoint.reset();
        mockEndpoint.expectedMessageCount(1);

        Object payload;
        switch (type) {
        case "bytes":
            payload = messageBody.getBytes(StandardCharsets.UTF_8);
            break;
        case "file":
            java.nio.file.Path path = Files.createTempFile("jms", ".txt");
            Files.write(path, messageBody.getBytes(StandardCharsets.UTF_8));
            payload = path.toFile();
            break;
        case "node":
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document document = factory.newDocumentBuilder().newDocument();
            Element element = document.createElement("test");
            element.setTextContent(messageBody);
            payload = element;
            break;
        case "string":
            payload = messageBody;
            break;
        default:
            throw new IllegalArgumentException("Unknown type: " + type);
        }

        producerTemplate.sendBody(componentScheme + ":queue:" + queueName, payload);

        mockEndpoint.assertIsSatisfied(5000);

        Exchange exchange = mockEndpoint.getReceivedExchanges().get(0);
        jakarta.jms.Message message = messageResolver.resolve(exchange);

        Object result;
        if ("string".equals(type) || "node".equals(type)) {
            assert message instanceof jakarta.jms.TextMessage;
            TextMessage textMessage = (TextMessage) message;
            result = textMessage.getText();
        } else {
            assert message instanceof jakarta.jms.BytesMessage;
            BytesMessage byteMessage = (BytesMessage) message;
            byteMessage.reset();
            byte[] byteData;
            byteData = new byte[(int) byteMessage.getBodyLength()];
            byteMessage.readBytes(byteData);
            result = new String(byteData);
        }

        return Response.ok().entity(result).build();
    }

    @Path("/{queueName}/map")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response jmsMapMessage(@PathParam("queueName") String queueName, Map<String, String> payload) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:mapResult", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        producerTemplate.sendBody(componentScheme + ":queue:" + queueName, payload);

        mockEndpoint.assertIsSatisfied(5000);

        Exchange exchange = mockEndpoint.getReceivedExchanges().get(0);
        MapMessage mapMessage = (MapMessage) messageResolver.resolve(exchange);

        Map<String, String> result = new HashMap<>();
        Enumeration<String> mapNames = mapMessage.getMapNames();
        while (mapNames.hasMoreElements()) {
            String key = mapNames.nextElement();
            result.put(key, mapMessage.getString(key));
        }

        return Response.ok().entity(result).build();
    }

    @Path("{queueName}/selector/{expression}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String jmsSelector(@PathParam("queueName") String queueName, @PathParam("expression") String expression) {
        producerTemplate.sendBodyAndHeader(componentScheme + ":queue:" + queueName,
                "Camel JMS Selector Not Matched",
                "foo", "baz");
        producerTemplate.sendBodyAndHeader(componentScheme + ":queue:" + queueName,
                "Camel JMS Selector Match",
                "foo", "bar");

        return consumerTemplate.receiveBody(
                componentScheme + ":" + queueName + "2?selector=" + expression, 5000,
                String.class);
    }

    @Path("/{queueName}/transaction")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response jmsTransaction(@PathParam("queueName") String queueName) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:txResult", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        mockEndpoint.message(0).header("count").isEqualTo(3);
        mockEndpoint.message(0).header(Exchange.REDELIVERED).isEqualTo(true);
        mockEndpoint.message(0).header(Exchange.REDELIVERY_COUNTER).isEqualTo(2);
        mockEndpoint.message(0).header("JMSRedelivered").isEqualTo(false);

        producerTemplate.sendBody(componentScheme + ":queue:" + queueName + "?transacted=true",
                "Test JMS Transaction");

        mockEndpoint.assertIsSatisfied(5000);

        Exchange exchange = mockEndpoint.getExchanges().get(0);
        Message message = exchange.getMessage();

        return Response.ok().entity(message.getBody()).build();
    }

    @Path("/{queueName}/object")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response testObjectMessage(@PathParam("queueName") String queueName, String name) throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:objectTestResult", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        producerTemplate.sendBody(componentScheme + ":queue:" + queueName, new Person(name));

        mockEndpoint.assertIsSatisfied();

        List<Exchange> exchanges = mockEndpoint.getExchanges();
        Exchange exchange = exchanges.get(0);
        Person body = exchange.getMessage().getBody(Person.class);

        return Response.ok().entity(body.getName()).build();
    }

    @Path("/topic/{topicName}")
    @Consumes(MediaType.TEXT_PLAIN)
    @POST
    public void topicPubSub(@PathParam("topicName") String topicName, String message) throws Exception {
        MockEndpoint topicResultA = context.getEndpoint("mock:topicResultA", MockEndpoint.class);
        topicResultA.expectedBodiesReceived(message);

        MockEndpoint topicResultB = context.getEndpoint("mock:topicResultB", MockEndpoint.class);
        topicResultB.expectedBodiesReceived(message);

        producerTemplate.sendBody(componentScheme + ":topic:" + topicName, message);

        topicResultA.assertIsSatisfied(5000);
        topicResultB.assertIsSatisfied(5000);
    }

    @Path("/mock/{name}/{count}/{timeout}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<String> mock(@PathParam("name") String name, @PathParam("count") int count, @PathParam("timeout") int timeout) {
        MockEndpoint mock = context.getEndpoint("mock:" + name, MockEndpoint.class);
        mock.setExpectedMessageCount(count);
        try {
            mock.assertIsSatisfied(timeout);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
        }
        return mock.getExchanges().stream().map(e -> e.getMessage().getBody(String.class)).collect(Collectors.toList());
    }

    @Path("/reply/to")
    @Consumes(MediaType.TEXT_PLAIN)
    @POST
    public void replyTo(String message) throws InterruptedException {
        MockEndpoint mockEndpointA = context.getEndpoint("mock:replyToStart", MockEndpoint.class);
        MockEndpoint mockEndpointB = context.getEndpoint("mock:replyToEnd", MockEndpoint.class);
        MockEndpoint mockEndpointC = context.getEndpoint("mock:replyToDone", MockEndpoint.class);

        mockEndpointA.expectedBodiesReceived(message);
        mockEndpointB.expectedBodiesReceived("Hello " + message);
        mockEndpointC.expectedBodiesReceived(message);

        producerTemplate.sendBody("direct:replyTo", message);

        mockEndpointA.assertIsSatisfied(5000L);
        mockEndpointB.assertIsSatisfied(5000L);
        mockEndpointC.assertIsSatisfied(5000L);
    }

    @Path("/pojo/consumer")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String pojoConsumer() {
        return pojoConsumer.getMessage(5000);
    }

    @Path("/routes/start")
    @GET
    public void startRoutes() {
        try {
            context.getRouteController().startAllRoutes();
        } catch (Exception e) {
            throw new RuntimeException("Unable to start camel routes", e);
        }
    }
}
