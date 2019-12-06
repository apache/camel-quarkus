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
package org.apache.camel.quarkus.component.telegram.it;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.telegram.TelegramConstants;
import org.apache.camel.component.telegram.TelegramMediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/telegram")
@ApplicationScoped
public class TelegramResource {

    private static final Logger log = Logger.getLogger(TelegramResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = "telegram.chatId")
    String chatId;

    @Path("/messages")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessages() {
        final String messages = consumerTemplate.receiveBodyNoWait("telegram://bots", String.class);
        log.infof("Received telegram messages: %s", messages);
        return messages;
    }

    @Path("/messages")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postMessage(String message) throws Exception {
        producerTemplate.requestBody(String.format("telegram://bots?chatId=%s", chatId), message);
        log.infof("Sent a message to telegram %s", message);
        return Response
                .created(new URI(String.format("https://telegram.org/")))
                .build();
    }

    @Path("/media")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response postMedia(@HeaderParam("Content-type") String type, byte[] message) throws Exception {
        final TelegramMediaType telegramMediaType;
        if (type != null && type.startsWith("image/")) {
            telegramMediaType = TelegramMediaType.PHOTO_PNG;
        } else if (type != null && type.startsWith("audio/")) {
            telegramMediaType = TelegramMediaType.AUDIO;
        } else if (type != null && type.startsWith("video/")) {
            telegramMediaType = TelegramMediaType.VIDEO;
        } else if (type != null && type.startsWith("application/pdf")) {
            telegramMediaType = TelegramMediaType.DOCUMENT;
        } else {
            return Response.status(415, "Unsupported content type " + type).build();
        }

        producerTemplate.requestBodyAndHeader(
                String.format("telegram://bots?chatId=%s", chatId),
                message,
                TelegramConstants.TELEGRAM_MEDIA_TYPE,
                telegramMediaType);
        log.infof("Sent a message to telegram %s", message);
        return Response
                .created(new URI(String.format("https://telegram.org/")))
                .build();
    }
}
