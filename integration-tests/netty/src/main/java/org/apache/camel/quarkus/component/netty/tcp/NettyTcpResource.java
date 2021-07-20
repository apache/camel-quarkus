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
package org.apache.camel.quarkus.component.netty.tcp;

import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.netty.buffer.ByteBuf;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.netty.NettyCamelStateCorrelationManager;

@Path("/netty/tcp")
public class NettyTcpResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Inject
    NettyCamelStateCorrelationManager correlationManager;

    @POST
    public String sendNettyTcpMessage(String message) {
        return producerTemplate.requestBody("netty:tcp://localhost:{{camel.netty.test-tcp-port}}?textline=true&sync=true",
                message, String.class);
    }

    @Path("/bytebuf")
    @POST
    public String sendNettyTcpMessageWithByteBufResponse(String message) {
        ByteBuf byteBuf = producerTemplate.requestBody(
                "netty:tcp://localhost:{{camel.netty.test-bytebuf-tcp-port}}?sync=true&useByteBuf=true", message,
                ByteBuf.class);
        return byteBuf.toString(StandardCharsets.UTF_8);
    }

    @Path("/codec")
    @POST
    public Object sendNettyTcpMessageWithCodec(String message) {
        producerTemplate.sendBody("netty:tcp://localhost:{{camel.netty.test-codec-tcp-port}}?disconnect=true"
                + "&sync=false&allowDefaultCodec=false"
                + "&decoders=#tcpNullDelimitedHandler,#bytesDecoder"
                + "&encoders=#bytesEncoder", createNullDelimitedMessage(message));

        return consumerTemplate.receiveBody("seda:custom-tcp-codec", 5000, String.class);
    }

    @Path("/ssl")
    @POST
    public String sendNettyTcpSSLMessage(String message) {
        return producerTemplate.requestBody(
                "netty:tcp://localhost:{{camel.netty.test-ssl-tcp-port}}?textline=true&sync=true&ssl=true&sslContextParameters=#sslContextParameters",
                message, String.class);
    }

    @Path("/server/initializer")
    @POST
    public String sendNettyTcpMessageWithServerInitializer(String message) {
        return producerTemplate.requestBody("netty:tcp://localhost:{{camel.netty.test-server-initializer-tcp-port}}?sync=true",
                message, String.class);
    }

    @Path("/client/initializer")
    @POST
    public String sendNettyTcpMessageWithClientInitializer(String message) {
        return producerTemplate.requestBody(
                "netty:tcp://localhost:{{camel.netty.test-tcp-port}}?textline=true&sync=true&clientInitializerFactory=#clientInitializerFactory",
                message, String.class);
    }

    @Path("/custom/thread/pools")
    @POST
    public String sendNettyTcpMessageWithCustomThreadPools(String message) {
        return producerTemplate.requestBody(
                "netty:tcp://localhost:{{camel.netty.test-worker-group-tcp-port}}?textline=true&sync=true&workerGroup=#clientWorkerGroup",
                message, String.class);
    }

    @Path("/custom/correlation/manager")
    @POST
    public void sendNettyTcpMessageWithCustomCorrelationManager() throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:correlationManagerTcp", MockEndpoint.class);

        mockEndpoint.expectedBodiesReceivedInAnyOrder("Bye A", "Bye B", "Bye C");
        mockEndpoint.allMessages().header("manager").isEqualTo(correlationManager);
        mockEndpoint.allMessages().predicate(exchange -> {
            String request = exchange.getMessage().getHeader("request", String.class);
            String reply = exchange.getMessage().getBody(String.class);
            return reply.endsWith(request);
        });

        producerTemplate.sendBodyAndHeader("seda:correlationManagerTcp", "A", "request", "A");
        producerTemplate.sendBodyAndHeader("seda:correlationManagerTcp", "B", "request", "B");
        producerTemplate.sendBodyAndHeader("seda:correlationManagerTcp", "C", "request", "C");

        mockEndpoint.assertIsSatisfied(5000L);
    }

    @Path("/object/serialize")
    @POST
    public void sendNettyTcpMessageWithTransferExchange(String message) throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:tcpObjectResult", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived(message);
        mockEndpoint.expectedHeaderReceived("foo", "bar");
        mockEndpoint.expectedPropertyReceived("cheese", "wine");

        producerTemplate.send(
                "netty:tcp://localhost:{{camel.netty.test-serialization-tcp-port}}?sync=true&transferExchange=true&encoders=#tcpObjectEncoder&decoders=#tcpObjectDecoder",
                new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getMessage();
                        in.setBody(message);
                        in.setHeader("foo", "bar");
                        exchange.setProperty("cheese", "wine");
                    }
                });

        mockEndpoint.assertIsSatisfied(5000L);
    }

    private byte[] createNullDelimitedMessage(String message) {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[messageBytes.length + 2];
        bytes[message.length() - 1] = 0;
        bytes[message.length() - 2] = 0;

        for (int i = 0; i < messageBytes.length; i++) {
            bytes[i] = messageBytes[i];
        }

        return bytes;
    }
}
