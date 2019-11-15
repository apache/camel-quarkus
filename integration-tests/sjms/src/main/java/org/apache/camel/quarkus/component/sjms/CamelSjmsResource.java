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
package org.apache.camel.quarkus.component.sjms;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/test")
@ApplicationScoped
public class CamelSjmsResource {
    @Inject
    ConnectionFactory connectionFactory;

    @Path("/jms/{queueName}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject post(@PathParam("queueName") String queueName, String message) {
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
            final JMSProducer producer = context.createProducer();
            final Queue queue = context.createQueue(queueName);

            producer.send(queue, message);
        }

        return Json.createObjectBuilder().build();
    }

    @Path("/jms/{queueName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject get(@PathParam("queueName") String queueName) {
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
                JMSConsumer consumer = context.createConsumer(context.createQueue(queueName))) {

            Message msg = consumer.receive(10000L);
            String body = msg != null ? msg.getBody(String.class) : "";

            return Json.createObjectBuilder()
                    .add("queueName", queueName)
                    .add("body", body)
                    .build();
        } catch (JMSException e) {
            throw new RuntimeException("Could not receive message", e);
        }
    }
}
