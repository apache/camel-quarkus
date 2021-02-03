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
package org.apache.camel.quarkus.component.syslog.it;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
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
import org.apache.camel.component.syslog.SyslogMessage;
import org.apache.camel.component.syslog.netty.Rfc5425Encoder;
import org.apache.camel.component.syslog.netty.Rfc5425FrameDecoder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/syslog")
public class SyslogResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = "camel.netty.rfc5425.port")
    int syslogRfc5425ServerPort;

    @ConfigProperty(name = "camel.netty.rfc3164.port")
    int syslogRfc3164ServerPort;

    @Path("/send/{version}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response sendMessage(@PathParam("version") String version, String message) throws Exception {
        int port = version.equals("rfc5425") ? syslogRfc5425ServerPort : syslogRfc3164ServerPort;
        producerTemplate.sendBody(
                "netty:udp://127.0.0.1:" + port + "?sync=false&allowDefaultCodec=false&useByteBuf=true",
                message.getBytes(StandardCharsets.UTF_8));
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/messages")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSyslogMessage() {
        SyslogMessage syslogMessage = consumerTemplate.receiveBody("seda:syslog-unmarshalled", 10000, SyslogMessage.class);

        Map<String, Object> syslogInfo = new HashMap<>();
        syslogInfo.put("hostname", syslogMessage.getHostname());
        syslogInfo.put("logMessage", syslogMessage.getLogMessage());
        syslogInfo.put("timestamp", syslogMessage.getTimestamp());

        return Response.ok(syslogInfo).build();
    }

    @Path("/messages/raw")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getRawMessage() {
        return consumerTemplate.receiveBody("seda:syslog-marshalled", 10000, String.class);
    }

    @javax.enterprise.inject.Produces
    @Named
    public Rfc5425Encoder rfc5425Encoder() {
        return new Rfc5425Encoder();
    }

    @javax.enterprise.inject.Produces
    @Named
    public Rfc5425FrameDecoder rfc5425FrameDecoder() {
        return new Rfc5425FrameDecoder();
    }

}
