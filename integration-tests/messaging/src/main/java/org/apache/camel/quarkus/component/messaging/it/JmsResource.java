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

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;

@Path("/messaging")
public class JmsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    // *****************************
    //
    // camel-jms
    //
    // *****************************

    @Path("/jms/{queueName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumeJmsMessage(@PathParam("queueName") String queueName) {
        return consumerTemplate.receiveBody("jms:queue:" + queueName, 5000, String.class);
    }

    @Path("/jms/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response produceJmsMessage(@PathParam("queueName") String queueName, String message) throws Exception {
        producerTemplate.sendBody("jms:queue:" + queueName, message);
        return Response.created(new URI("https://camel.apache.org/")).build();
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
