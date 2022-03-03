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
package org.apache.camel.quarkus.component.mail;

import java.io.IOException;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.mail.Folder;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.attachment.DefaultAttachment;
import org.apache.camel.component.mail.MailMessage;

@Path("/mail")
@ApplicationScoped
public class CamelResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Path("/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void sendMail(
            @QueryParam("subject") String subject,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            String body) {

        producerTemplate.send("direct:sendMail", exchange -> {
            Message message = exchange.getMessage();
            message.setHeader("Subject", subject);
            message.setHeader("From", from);
            message.setHeader("To", to);
            message.setBody(body);
        });
    }

    @Path("/send/attachment/{fileName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void sendMailWithAttachment(
            @PathParam("fileName") String fileName,
            @QueryParam("subject") String subject,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            String body) {

        producerTemplate.send("direct:sendMailWithAttachment", exchange -> {
            AttachmentMessage in = exchange.getMessage(AttachmentMessage.class);

            DefaultAttachment attachment = new DefaultAttachment(new FileDataSource(fileName));
            in.addAttachmentObject(fileName, attachment);

            Message message = exchange.getMessage();
            message.setHeader("Subject", subject);
            message.setHeader("From", from);
            message.setHeader("To", to);
            message.setBody(body);
        });
    }

    @Path("/mimeMultipartUnmarshalMarshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String mimeMultipartUnmarshalMarshal(String body) {
        return producerTemplate.requestBody("direct:mimeMultipartUnmarshalMarshal", body, String.class);
    }

    @Path("/mimeMultipartMarshal/{fileName}/{fileContent}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String mimeMultipart(
            @PathParam("fileName") String fileName,
            @PathParam("fileContent") String fileContent,
            String body) {

        return producerTemplate.request("direct:mimeMultipartMarshal", e -> {
            AttachmentMessage in = e.getMessage(AttachmentMessage.class);
            in.setBody(body);
            in.setHeader(Exchange.CONTENT_TYPE, "text/plain;charset=iso8859-1;other-parameter=true");
            in.setHeader(Exchange.CONTENT_ENCODING, "UTF8");

            DefaultAttachment attachment = new DefaultAttachment(new ByteArrayDataSource(fileContent, "text/plain"));
            attachment.addHeader("Content-Description", "Sample Attachment Data");
            attachment.addHeader("X-AdditionalData", "additional data");
            in.addAttachmentObject(fileName, attachment);

        }).getMessage().getBody(String.class);
    }

    @Path("/inbox/{protocol}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getMail(@PathParam("protocol") String protocol) throws Exception {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        Exchange exchange = consumerTemplate.receive("seda:mail-" + protocol, 5000);
        if (exchange != null) {
            MailMessage mailMessage = exchange.getMessage(MailMessage.class);
            AttachmentMessage attachmentMessage = exchange.getMessage(AttachmentMessage.class);
            Map<String, DataHandler> attachments = attachmentMessage.getAttachments();
            if (attachments != null) {
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                attachments.forEach((id, dataHandler) -> {
                    JsonObjectBuilder attachmentObject = Json.createObjectBuilder();
                    attachmentObject.add("attachmentFilename", dataHandler.getName());

                    try {
                        String content = context.getTypeConverter().convertTo(String.class, dataHandler.getInputStream());
                        attachmentObject.add("attachmentContent", content);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }

                    arrayBuilder.add(attachmentObject.build());
                });

                builder.add("attachments", arrayBuilder.build());
            }

            Folder folder = mailMessage.getOriginalMessage().getFolder();
            if (!folder.isOpen()) {
                folder.open(Folder.READ_ONLY);
            }

            builder.add("subject", mailMessage.getMessage().getSubject());
            builder.add("content", mailMessage.getBody(String.class).trim());
        }

        return builder.build();
    }
}
