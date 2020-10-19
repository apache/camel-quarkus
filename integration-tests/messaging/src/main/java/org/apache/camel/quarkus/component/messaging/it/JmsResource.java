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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.jms.MapMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response jmsMessageType(@PathParam("type") String type) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:jmsType", MockEndpoint.class);
        mockEndpoint.reset();
        mockEndpoint.expectedMessageCount(1);

        producerTemplate.sendBodyAndHeader("jms:queue:typeTest", "test message payload", "type", type);

        mockEndpoint.assertIsSatisfied(5000);

        javax.jms.Message message = mockEndpoint.getReceivedExchanges()
                .get(0)
                .getMessage(JmsMessage.class)
                .getJmsMessage();

        boolean result;
        if (type.equals("Text")) {
            result = (message instanceof javax.jms.TextMessage);
        } else {
            result = (message instanceof javax.jms.BytesMessage);
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
    @Produces(MediaType.TEXT_PLAIN)
    public String customMessageListenerContainerFactory(String message) {
        producerTemplate.sendBody("jms:queue:listener?messageListenerContainerFactory=#customMessageListener", message);
        return consumerTemplate.receiveBody("jms:queue:listener", 5000, String.class);
    }

    @Path("/jms/custom/destination/resolver")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String customDestinationResolver(String message) {
        producerTemplate.sendBody("jms:queue:ignored?destinationResolver=#customDestinationResolver", message);

        // The custom DestinationResolver should have overridden the original queue name to 'destinationOverride'
        return consumerTemplate.receiveBody("jms:queue:destinationOverride", 5000, String.class);
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

        mockEndpoint.assertIsSatisfied(15000);

        Exchange exchange = mockEndpoint.getExchanges().get(0);
        Message message = exchange.getMessage();

        return Response.ok().entity(message.getBody()).build();
    }

    @Path("/jms/topic")
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
