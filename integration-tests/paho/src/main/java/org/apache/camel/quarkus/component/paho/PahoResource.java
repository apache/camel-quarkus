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

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
import org.apache.camel.component.paho.PahoConstants;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jboss.logging.Logger;

@Path("/paho")
@ApplicationScoped
public class PahoResource {

    private static final Logger LOG = Logger.getLogger(PahoResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    private static final String KEYSTORE_FILE = "clientkeystore.jks";
    private static final String KEYSTORE_PASSWORD = "quarkus";

    @Path("/{protocol}/{queueName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumePahoMessage(@PathParam("protocol") String protocol, @PathParam("queueName") String queueName) {
        java.nio.file.Path keyStore = null;
        if ("ssl".equals(protocol)) {
            keyStore = copyKeyStore();
        }
        try {
            return consumerTemplate.receiveBody(
                    "paho:" + queueName + "?brokerUrl=" + brokerUrl(protocol) + sslOptions(keyStore), 5000, String.class);
        } finally {
            if ("ssl".equals(protocol)) {
                removeKeyStore(keyStore);
            }
        }
    }

    @Path("/{protocol}/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response producePahoMessage(@PathParam("protocol") String protocol, @PathParam("queueName") String queueName,
            String message) throws Exception {
        java.nio.file.Path keyStore = null;
        if ("ssl".equals(protocol)) {
            keyStore = copyKeyStore();
        }
        try {
            producerTemplate.sendBody(
                    "paho:" + queueName + "?retained=true&brokerUrl=" + brokerUrl(protocol) + sslOptions(keyStore), message);
        } finally {
            if ("ssl".equals(protocol)) {
                removeKeyStore(keyStore);
            }
        }
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/override/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response overrideQueueName(@PathParam("queueName") String queueName, String message) throws Exception {
        producerTemplate.sendBodyAndHeader("paho:test?retained=true&brokerUrl=" + brokerUrl("tcp"), message,
                PahoConstants.CAMEL_PAHO_OVERRIDE_TOPIC, queueName);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    /**
     * This method simulates the case where an MqqtException is thrown during a
     * reconnection attempt in the MqttCallbackExtended instance set by the
     * PahoConsumer on endpoint startup.
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
        producerTemplate.requestBody("paho:withFilePersistence?retained=true&persistence=FILE&brokerUrl=" + brokerUrl("tcp"),
                message);
        return consumerTemplate.receiveBody("paho:withFilePersistence?persistence=FILE&brokerUrl=" + brokerUrl("tcp"), 5000,
                String.class);
    }

    @Path("/sendReceiveWithRfc3986AuthorityShouldSucceed")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sendReceiveWithRfc3986AuthorityShouldSucceed(@QueryParam("message") String message) {

        // Change the brokerUrl to an RFC3986 form
        String tcpUrl = ConfigProvider.getConfig().getValue("paho.broker.tcp.url", String.class);
        tcpUrl = tcpUrl.replaceAll("tcp://([^:]*):(.*)", "tcp://user:password@$1:$2");

        producerTemplate.requestBody("paho:rfc3986?retained=true&brokerUrl=" + tcpUrl,
                message);
        return consumerTemplate.receiveBody("paho:rfc3986?brokerUrl=" + tcpUrl, 5000,
                String.class);
    }

    private String brokerUrl(String protocol) {
        return ConfigProvider.getConfig().getValue("paho.broker." + protocol + ".url", String.class);
    }

    private String sslOptions(java.nio.file.Path keyStore) {
        return keyStore == null
                ? ""
                : "&sslClientProps.com.ibm.ssl.keyStore=" + keyStore +
                        "&sslClientProps.com.ibm.ssl.keyStorePassword=" + KEYSTORE_PASSWORD +
                        "&sslClientProps.com.ibm.ssl.trustStore=" + keyStore +
                        "&sslClientProps.com.ibm.ssl.trustStorePassword=" + KEYSTORE_PASSWORD;
    }

    private java.nio.file.Path copyKeyStore() {
        java.nio.file.Path tmpKeystore = null;
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(KEYSTORE_FILE);) {
            tmpKeystore = Files.createTempFile("keystore-", ".jks");
            Files.copy(in, tmpKeystore, StandardCopyOption.REPLACE_EXISTING);
            return tmpKeystore;
        } catch (Exception e) {
            throw new RuntimeException("Could not copy " + KEYSTORE_FILE + " from the classpath to " + tmpKeystore, e);
        }
    }

    private void removeKeyStore(java.nio.file.Path keystore) {
        try {
            Files.delete(keystore);
        } catch (Exception e) {
            throw new RuntimeException("Could not delete " + keystore, e);
        }
    }
}
