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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.paho.mqtt5.PahoMqtt5Constants;
import org.apache.camel.spi.RouteController;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

@Path("/paho-mqtt5")
@ApplicationScoped
public class PahoMqtt5Resource {
    private static final Logger LOG = Logger.getLogger(PahoMqtt5Resource.class);

    @Inject
    CamelContext context;

    @Inject
    Counter counter;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    private final String keystore = "target/certs/paho-mqtt5-keystore.p12";
    public final static String KEYSTORE_PASSWORD = "quarkus";

    @Path("/{protocol}/{queueName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumePahoMessage(
            @PathParam("protocol") String protocol,
            @PathParam("queueName") String queueName) {
        String tmpKeystore = null;
        String sslClientProps = "";
        String result;

        try {
            if ("ssl".equals(protocol)) {
                tmpKeystore = setKeyStore(keystore);
                sslClientProps = "&httpsHostnameVerificationEnabled=false" +
                        "&sslHostnameVerifier=#hostnameVerifier" +
                        "&sslClientProps.com.ibm.ssl.keyStore=" + tmpKeystore +
                        "&sslClientProps.com.ibm.ssl.keyStorePassword=" + KEYSTORE_PASSWORD +
                        "&sslClientProps.com.ibm.ssl.trustStore=" + tmpKeystore +
                        "&sslClientProps.com.ibm.ssl.trustStorePassword=" + KEYSTORE_PASSWORD;
            }
            result = consumerTemplate.receiveBody(
                    "paho-mqtt5:" + queueName + "?brokerUrl=" + brokerUrl(protocol) + sslClientProps, 5000,
                    String.class);
        } finally {
            if ("ssl".equals(protocol) && tmpKeystore != null) {
                removeKeyStore(tmpKeystore);
            }
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
        String tmpKeystore = null;
        String sslClientProps = "";

        try {
            if ("ssl".equals(protocol)) {
                tmpKeystore = setKeyStore(keystore);
                sslClientProps = "&httpsHostnameVerificationEnabled=false" +
                        "&sslHostnameVerifier=#hostnameVerifier" +
                        "&sslClientProps.com.ibm.ssl.keyStore=" + tmpKeystore +
                        "&sslClientProps.com.ibm.ssl.keyStorePassword=" + KEYSTORE_PASSWORD +
                        "&sslClientProps.com.ibm.ssl.trustStore=" + tmpKeystore +
                        "&sslClientProps.com.ibm.ssl.trustStorePassword=" + KEYSTORE_PASSWORD;
            }
            producerTemplate.sendBody(
                    "paho-mqtt5:" + queueName + "?retained=true&brokerUrl=" + brokerUrl(protocol) + sslClientProps, message);
        } finally {
            if ("ssl".equals(protocol) && tmpKeystore != null) {
                removeKeyStore(tmpKeystore);
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

    @Singleton
    @Named
    HostnameVerifier hostnameVerifier() {
        // Allow certificate host name validation to work with whatever container host name is used
        return new HostnameVerifier() {
            final Config config = ConfigProvider.getConfig();

            @Override
            public boolean verify(String hostname, SSLSession session) {
                return config.getValue("paho5.broker.host", String.class).equals(hostname);
            }
        };
    }

    private String brokerUrl(String protocol) {
        return ConfigProvider.getConfig().getValue("paho5.broker." + protocol + ".url", String.class);
    }

    private String setKeyStore(String keystore) {
        String tmpKeystore = null;

        try (InputStream in = new FileInputStream(Paths.get(keystore).toFile())) {
            tmpKeystore = File.createTempFile("keystore-", ".jks").getPath();
            Files.copy(in, Paths.get(tmpKeystore), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Could not copy " + keystore + " from the classpath to " + tmpKeystore, e);
        } finally {
            return tmpKeystore;
        }
    }

    private void removeKeyStore(String keystore) {
        try {
            Files.deleteIfExists(Paths.get(keystore));
        } catch (Exception e) {
            LOG.warn("Failed to delete temporary keystore file", e);
        }
    }
}
