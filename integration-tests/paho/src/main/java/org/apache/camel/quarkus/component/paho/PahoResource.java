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
package org.apache.camel.quarkus.component.paho;

import java.net.URI;

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

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.eclipse.paho.client.mqttv3.MqttException;

@Path("/paho")
@ApplicationScoped
public class PahoResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/paho/{queueName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumePahoMessage(@PathParam("queueName") String queueName) {
        return consumerTemplate.receiveBody("paho:" + queueName, 5000,
                String.class);
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
    public String consumePahoWsMessage(@PathParam("queueName") String queueName) {
        return consumerTemplate.receiveBody("paho:" + queueName + "?brokerUrl={{broker-url.ws}}", 5000,
                String.class);
    }

    @Path("/paho-ws/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response producePahoWsMessage(@PathParam("queueName") String queueName, String message) throws Exception {
        producerTemplate.sendBody("paho:" + queueName + "?retained=true&brokerUrl={{broker-url.ws}}", message);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    /**
     * This method simulates the case where an MqqtException is thrown during a reconnection attempt
     * in the MqttCallbackExtended instance set by the PahoConsumer on endpoint startup.
     */
    @Path("/mqttExceptionDuringReconnectShouldSucceed")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String mqttExceptionDuringReconnectShouldSucceed() {
        MqttException mqex = new MqttException(MqttException.REASON_CODE_BROKER_UNAVAILABLE);
        return mqex.getMessage();
    }

    @Path("/readThenWriteWithFilePersistenceShouldSucceed")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String readThenWriteWithFilePersistenceShouldSucceed(@QueryParam("message") String message) throws Exception {
        producerTemplate.requestBody("paho:withFilePersistence?retained=true&persistence=FILE", message);
        return consumerTemplate.receiveBody("paho:withFilePersistence?persistence=FILE", 5000, String.class);
    }
}
