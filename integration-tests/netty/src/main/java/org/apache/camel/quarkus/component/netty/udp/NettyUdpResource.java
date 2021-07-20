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
package org.apache.camel.quarkus.component.netty.udp;

import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.netty.NettyCamelStateCorrelationManager;

@Path("/netty/udp")
public class NettyUdpResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Inject
    NettyCamelStateCorrelationManager correlationManager;

    @POST
    public String sendNettyUdpMessage(String message) {
        return producerTemplate.requestBody("netty:udp://localhost:{{camel.netty.test-udp-port}}?sync=true", message,
                String.class);
    }

    @Path("/codec")
    @POST
    public Object sendNettyUdpMessageWithCodec(String message) {
        producerTemplate.sendBody("netty:udp://localhost:{{camel.netty.test-codec-udp-port}}?sync=false"
                + "&udpByteArrayCodec=true", message.getBytes(StandardCharsets.UTF_8));

        return consumerTemplate.receiveBody("seda:custom-udp-codec", 5000, String.class);
    }

    @Path("/server/initializer")
    @POST
    public String sendNettyUdpMessageWithServerInitializer(String message) {
        return producerTemplate.requestBody("netty:udp://localhost:{{camel.netty.test-server-initializer-udp-port}}?sync=true",
                message, String.class);
    }

    @Path("/client/initializer")
    @POST
    public String sendNettyUdpMessageWithClientInitializer(String message) {
        return producerTemplate.requestBody(
                "netty:udp://localhost:{{camel.netty.test-udp-port}}?sync=true&clientInitializerFactory=#clientInitializerFactory",
                message, String.class);

    }

    @Path("/custom/thread/pools")
    @POST
    public String sendNettyUdpMessageWithCustomThreadPools(String message) {
        return producerTemplate.requestBody(
                "netty:udp://localhost:{{camel.netty.test-worker-group-udp-port}}?sync=true&workerGroup=#clientWorkerGroup",
                message, String.class);
    }

    @Path("/object/serialize")
    @POST
    public void sendNettyTcpMessageWithTransferExchange(String message) throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:udpObjectResult", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived(message);
        mockEndpoint.expectedHeaderReceived("foo", "bar");
        mockEndpoint.expectedPropertyReceived("cheese", "wine");

        producerTemplate.send(
                "netty:udp://localhost:{{camel.netty.test-serialization-udp-port}}?sync=true&transferExchange=true&encoders=#udpObjectEncoder&decoders=#udpObjectDecoder",
                exchange -> {
                    Message in = exchange.getMessage();
                    in.setBody(message);
                    in.setHeader("foo", "bar");
                    exchange.setProperty("cheese", "wine");
                });

        mockEndpoint.assertIsSatisfied(5000L);
    }
}
