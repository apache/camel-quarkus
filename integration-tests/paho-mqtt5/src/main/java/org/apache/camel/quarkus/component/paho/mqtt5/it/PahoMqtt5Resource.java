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
package org.apache.camel.quarkus.component.paho.mqtt5.it;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.paho.mqtt5.PahoMqtt5Constants;
import org.apache.camel.spi.RouteController;
import org.eclipse.microprofile.config.ConfigProvider;

@Path("/paho-mqtt5")
@ApplicationScoped
public class PahoMqtt5Resource {

    @Inject
    CamelContext context;

    @Inject
    Counter counter;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    private final String keystore = "clientkeystore.jks";
    private final String password = "quarkus";

    @Path("/{protocol}/{queueName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumePahoMessage(
            @PathParam("protocol") String protocol,
            @PathParam("queueName") String queueName) {
        if ("ssl".equals(protocol)) {
            setKeyStore(keystore, password);
        }
        String result = consumerTemplate.receiveBody("paho-mqtt5:" + queueName + "?brokerUrl=" + brokerUrl(protocol), 5000,
                String.class);
        if ("ssl".equals(protocol)) {
            removeKeyStore(keystore);
        }
        return result;
    }

    @Path("/{protocol}/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response producePahoMessage(
            @PathParam("protocol") String protocol,
            @PathParam("queueName") String queueName,
            String message) throws Exception {
        if ("ssl".equals(protocol)) {
            setKeyStore(keystore, password);
        }
        try {
            producerTemplate.sendBody("paho-mqtt5:" + queueName + "?retained=true&brokerUrl=" + brokerUrl(protocol), message);
        } finally {
            if ("ssl".equals(protocol)) {
                removeKeyStore(keystore);
            }
        }
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/override/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response overrideQueueName(
            @PathParam("queueName") String queueName,
            String message) throws Exception {
        producerTemplate.sendBodyAndHeader("paho-mqtt5:test?retained=true&brokerUrl=" + brokerUrl("tcp"), message,
                PahoMqtt5Constants.CAMEL_PAHO_OVERRIDE_TOPIC, queueName);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/readThenWriteWithFilePersistenceShouldSucceed")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String readThenWriteWithFilePersistenceShouldSucceed(@QueryParam("message") String message) throws Exception {
        producerTemplate.sendBody(
                "paho-mqtt5:withFilePersistence?retained=true&persistence=FILE&brokerUrl="
                        + brokerUrl("tcp"),
                message);
        return consumerTemplate.receiveBody(
                "paho-mqtt5:withFilePersistence?persistence=FILE&brokerUrl=" + brokerUrl("tcp"),
                5000,
                String.class);
    }

    @Path("/routeStatus/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String routeStatus(@PathParam("id") String routeId,
            @QueryParam("waitForContainerStarted") @DefaultValue("false") boolean wait) throws Exception {
        RouteController routeController = context.getRouteController();
        if (wait) {
            counter.await(30, TimeUnit.SECONDS);
        }
        return routeController.getRouteStatus(routeId).name();
    }

    @Path("/mock")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String mock(String message) throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:test", MockEndpoint.class);
        endpoint.expectedBodiesReceived(message);

        endpoint.assertIsSatisfied();
        return "OK";
    }

    @Path("/send")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response send(String message) throws Exception {
        producerTemplate.sendBody("direct:test", message);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    private String brokerUrl(String protocol) {
        return ConfigProvider.getConfig().getValue("paho5.broker." + protocol + ".url", String.class);
    }

    private void setKeyStore(String keystore, String password) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(keystore);

        try {
            Files.copy(in, Paths.get(keystore));
        } catch (Exception e) {
        }

        System.setProperty("javax.net.ssl.keyStore", keystore);
        System.setProperty("javax.net.ssl.keyStorePassword", password);
        System.setProperty("javax.net.ssl.trustStore", keystore);
        System.setProperty("javax.net.ssl.trustStorePassword", password);
    }

    private void removeKeyStore(String keystore) {
        try {
            Files.delete(Paths.get(keystore));
        } catch (Exception e) {
        }

        System.clearProperty("javax.net.ssl.keyStore");
        System.clearProperty("javax.net.ssl.keyStorePassword");
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");
    }
}
