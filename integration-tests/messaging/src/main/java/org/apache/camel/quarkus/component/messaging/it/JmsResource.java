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

import javax.inject.Inject;
import javax.jms.BytesMessage;
import javax.jms.MapMessage;
import javax.jms.TextMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jms.JmsMessage;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/messaging")
public class JmsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    // *****************************
    //
    // camel-jms
    //
    // *****************************

    @Path("/jms/{queueName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumeJmsQueueMessage(@PathParam("queueName") String queueName) {
        return consumerTemplate.receiveBody("jms:queue:" + queueName, 5000, String.class);
    }

    @Path("/jms/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response produceJmsQueueMessage(@PathParam("queueName") String queueName, String message) throws Exception {
        producerTemplate.sendBody("jms:queue:" + queueName, message);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/jms/type/{type}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response jmsMessageType(@PathParam("type") String type, String messageBody) throws Exception {
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

        producerTemplate.sendBody("jms:queue:typeTest", payload);

        mockEndpoint.assertIsSatisfied(5000);

        javax.jms.Message message = mockEndpoint.getReceivedExchanges()
                .get(0)
                .getMessage(JmsMessage.class)
                .getJmsMessage();

        Object result;
        if (type.equals("string") || type.equals("node")) {
            assert message instanceof javax.jms.TextMessage;
            TextMessage textMessage = (TextMessage) message;
            result = textMessage.getText();
        } else {
            assert message instanceof javax.jms.BytesMessage;
            BytesMessage byteMessage = (BytesMessage) message;
            byteMessage.reset();
            byte[] byteData;
            byteData = new byte[(int) byteMessage.getBodyLength()];
            byteMessage.readBytes(byteData);
            result = new String(byteData);
        }

        return Response.ok().entity(result).build();
    }

    @Path("/jms/map")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jmsMapMessage(Map<String, String> payload) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:mapResult", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        producerTemplate.sendBody("jms:queue:mapTest", payload);

        mockEndpoint.assertIsSatisfied(5000);

        JmsMessage message = mockEndpoint.getReceivedExchanges()
                .get(0)
                .getMessage(JmsMessage.class);

        MapMessage mapMessage = (MapMessage) message.getJmsMessage();

        Map<String, String> result = new HashMap<>();
        Enumeration<String> mapNames = mapMessage.getMapNames();
        while (mapNames.hasMoreElements()) {
            String key = mapNames.nextElement();
            result.put(key, mapMessage.getString(key));
        }

        return Response.ok().entity(result).build();
    }

    @Path("/jms/custom/message/listener/factory")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String customMessageListenerContainerFactory(String message) {
        producerTemplate.sendBody("jms:queue:listener?messageListenerContainerFactory=#customMessageListener", message);
        return consumerTemplate.receiveBody("jms:queue:listener", 5000, String.class);
    }

    @Path("/jms/custom/destination/resolver")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String customDestinationResolver(String message) {
        producerTemplate.sendBody("jms:queue:ignored?destinationResolver=#customDestinationResolver", message);

        // The custom DestinationResolver should have overridden the original queue name to 'destinationOverride'
        return consumerTemplate.receiveBody("jms:queue:destinationOverride", 5000, String.class);
    }

    @Path("/jms/custom/message/converter")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String customMessageConverter(String message) {
        producerTemplate.sendBody("jms:queue:converter?messageConverter=#customMessageConverter", message);
        return consumerTemplate.receiveBody("jms:queue:converter?messageConverter=#customMessageConverter", 5000, String.class);
    }

    @Path("/jms/selector/{expression}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String jmsSelector(@PathParam("expression") String expression) {
        producerTemplate.sendBodyAndHeader("jms:queue:selectorA", "Camel JMS Selector Not Matched", "foo", "baz");
        producerTemplate.sendBodyAndHeader("jms:queue:selectorA", "Camel JMS Selector Match", "foo", "bar");
        return consumerTemplate.receiveBody("jms:queue:selectorB?selector=" + expression, 5000, String.class);
    }

    @Path("/jms/transaction")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response jmsTransaction() throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:txResult", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        mockEndpoint.message(0).header("count").isEqualTo(3);
        mockEndpoint.message(0).header(Exchange.REDELIVERED).isEqualTo(true);
        mockEndpoint.message(0).header(Exchange.REDELIVERY_COUNTER).isEqualTo(2);
        mockEndpoint.message(0).header("JMSRedelivered").isEqualTo(false);

        producerTemplate.sendBody("jms:queue:txTest?transacted=true", "Test JMS Transaction");

        mockEndpoint.assertIsSatisfied(5000);

        Exchange exchange = mockEndpoint.getExchanges().get(0);
        Message message = exchange.getMessage();

        return Response.ok().entity(message.getBody()).build();
    }

    @Path("/jms/object")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response testObjectMessage(String name) throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:objectTestResult", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        producerTemplate.sendBody("jms:queue:objectTest", new Person(name));

        mockEndpoint.assertIsSatisfied();

        List<Exchange> exchanges = mockEndpoint.getExchanges();
        Exchange exchange = exchanges.get(0);
        Person body = exchange.getMessage().getBody(Person.class);

        return Response.ok().entity(body.getName()).build();
    }

    @Path("/jms/transfer/exchange")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response testTransferExchange(String message) throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:transferExchangeResult", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        producerTemplate.sendBody("jms:queue:transferExchange?transferExchange=true", message);

        mockEndpoint.assertIsSatisfied();

        List<Exchange> exchanges = mockEndpoint.getExchanges();
        Exchange exchange = exchanges.get(0);
        String result = exchange.getMessage().getBody(String.class);

        return Response.ok().entity(result).build();
    }

    @Path("/jms/topic")
    @Consumes(MediaType.TEXT_PLAIN)
    @POST
    public void topicPubSub(String message) throws Exception {
        MockEndpoint topicResultA = context.getEndpoint("mock:topicResultA", MockEndpoint.class);
        topicResultA.expectedBodiesReceived(message);

        MockEndpoint topicResultB = context.getEndpoint("mock:topicResultB", MockEndpoint.class);
        topicResultB.expectedBodiesReceived(message);

        producerTemplate.sendBody("jms:topic:test", message);

        topicResultA.assertIsSatisfied(5000);
        topicResultB.assertIsSatisfied(5000);
    }

    @Path("/jms/mock/{name}/{count}/{timeout}")
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

    // *****************************
    //
    // camel-paho
    //
    // *****************************

    @Path("/paho/{queueName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumePahoMessage(@PathParam("queueName") String queueName) {
        return consumerTemplate.receiveBody("paho:" + queueName, 5000, String.class);
    }

    @Path("/paho/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response producePahoMessage(@PathParam("queueName") String queueName, String message) throws Exception {
        producerTemplate.sendBody("paho:" + queueName + "?retained=true", message);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/paho-ws/{queueName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumePahoMessageWs(@PathParam("queueName") String queueName) {
        return consumerTemplate.receiveBody("paho:" + queueName, 5000, String.class);
    }

    @Path("/paho-ws/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response producePahoMessageWs(@PathParam("queueName") String queueName, String message) throws Exception {
        producerTemplate.sendBody("paho:" + queueName + "?retained=true&brokerUrl={{broker-url.ws}}", message);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    // *****************************
    //
    // camel-sjms
    //
    // *****************************

    @Path("/sjms/{queueName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumeSjmsMessage(@PathParam("queueName") String queueName) {
        return consumerTemplate.receiveBody("sjms:queue:" + queueName, 5000, String.class);
    }

    @Path("/sjms/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response produceSjmsMessage(@PathParam("queueName") String queueName, String message) throws Exception {
        producerTemplate.sendBody("sjms2:queue:" + queueName, message);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }
}
