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
import java.util.Arrays;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.telegram.TelegramConstants;
import org.apache.camel.component.telegram.TelegramMediaType;
import org.apache.camel.component.telegram.model.EditMessageLiveLocationMessage;
import org.apache.camel.component.telegram.model.InlineKeyboardButton;
import org.apache.camel.component.telegram.model.OutgoingTextMessage;
import org.apache.camel.component.telegram.model.ReplyKeyboardMarkup;
import org.apache.camel.component.telegram.model.SendLocationMessage;
import org.apache.camel.component.telegram.model.SendVenueMessage;
import org.apache.camel.component.telegram.model.StopMessageLiveLocationMessage;
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

    @Inject
    CamelContext context;

    @ConfigProperty(name = "telegram.chatId")
    String chatId;

    @Path("/messages")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessages() {
        final String messages = consumerTemplate.receiveBody("telegram://bots", 5000L, String.class);
        log.infof("Received telegram messages: %s", messages);
        return messages;
    }

    @Path("/messages")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postMessage(String msg) throws Exception {
        OutgoingTextMessage message = new OutgoingTextMessage();
        message.setText(msg);

        // customize keyboard
        InlineKeyboardButton buttonOptionOneI = InlineKeyboardButton.builder()
                .text("Option One - I").build();

        InlineKeyboardButton buttonOptionOneII = InlineKeyboardButton.builder()
                .text("Option One - II").build();

        InlineKeyboardButton buttonOptionTwoI = InlineKeyboardButton.builder()
                .text("Option Two - I").build();

        ReplyKeyboardMarkup replyMarkup = ReplyKeyboardMarkup.builder()
                .keyboard()
                .addRow(Arrays.asList(buttonOptionOneI, buttonOptionOneII))
                .addRow(Arrays.asList(buttonOptionTwoI))
                .close()
                .oneTimeKeyboard(true)
                .build();

        message.setReplyMarkup(replyMarkup);

        producerTemplate.requestBody(String.format("telegram://bots?chatId=%s", chatId), message);
        log.infof("Sent a message to telegram %s", msg);
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

    @Path("/send-location")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendLocation(SendLocationMessage message) throws Exception {
        final Object result = producerTemplate.requestBody(String.format("telegram://bots?chatId=%s", chatId), message);
        log.infof("Sent a message to telegram %s", message);
        return Response
                .created(new URI(String.format("https://telegram.org/")))
                .entity(result)
                .build();
    }

    @Path("/edit-location")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editLocation(EditMessageLiveLocationMessage message) throws Exception {
        producerTemplate.requestBody(String.format("telegram://bots?chatId=%s", chatId), message);
        log.infof("Sent a message to telegram %s", message);
        return Response
                .created(new URI(String.format("https://telegram.org/")))
                .build();
    }

    @Path("/stop-location")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response stopLocation(StopMessageLiveLocationMessage message) throws Exception {
        producerTemplate.requestBody(String.format("telegram://bots?chatId=%s", chatId), message);
        log.infof("Sent a message to telegram %s", message);
        return Response
                .created(new URI(String.format("https://telegram.org/")))
                .build();
    }

    @Path("/venue")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response venue(SendVenueMessage message) throws Exception {
        final Object result = producerTemplate.requestBody(String.format("telegram://bots?chatId=%s", chatId), message);
        log.infof("Sent a message to telegram %s", message);
        return Response
                .created(new URI(String.format("https://telegram.org/")))
                .entity(result)
                .build();
    }

    @Path("/webhook")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String webhookMessages() {
        final MockEndpoint mockEndpoint = context.getEndpoint("mock:webhook", MockEndpoint.class);
        return mockEndpoint.getReceivedExchanges().stream()
                .map(Exchange::getMessage)
                .map(m -> m.getBody(String.class))
                .findFirst().orElse("");
    }

}
