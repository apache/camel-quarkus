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
package org.apache.camel.quarkus.component.netty;

import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;

@Path("/netty")
public class NettyResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/tcp")
    @POST
    public String sendNettyTcpMessage(String message) {
        return producerTemplate.requestBody("netty:tcp://localhost:{{camel.netty.test-tcp-port}}?textline=true&sync=true",
                message, String.class);
    }

    @Path("/udp")
    @POST
    public String sendNettyUdpMessage(String message) {
        return producerTemplate.requestBody("netty:udp://localhost:{{camel.netty.test-udp-port}}?sync=true", message,
                String.class);
    }

    @Path("/tcp/codec")
    @POST
    public Object sendNettyTcpMessageWithCodec(String message) {
        producerTemplate.sendBody("netty:tcp://localhost:{{camel.netty.test-codec-tcp-port}}?disconnect=true"
                + "&sync=false&allowDefaultCodec=false"
                + "&decoders=#tcpNullDelimitedHandler,#bytesDecoder"
                + "&encoders=#bytesEncoder", createNullDelimitedMessage(message));

        return consumerTemplate.receiveBody("seda:custom-tcp-codec", 5000, String.class);
    }

    @Path("/udp/codec")
    @POST
    public Object sendNettyUdpMessageWithCodec(String message) {
        producerTemplate.sendBody("netty:udp://localhost:{{camel.netty.test-codec-udp-port}}?sync=false"
                + "&udpByteArrayCodec=true", message.getBytes(StandardCharsets.UTF_8));

        return consumerTemplate.receiveBody("seda:custom-udp-codec", 5000, String.class);
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
