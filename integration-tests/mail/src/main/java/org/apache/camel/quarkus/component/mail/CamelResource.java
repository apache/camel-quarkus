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

import java.util.Map;

import javax.activation.DataHandler;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
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

    @Path("/route/{route}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String route(String statement, @PathParam("route") String route) throws Exception {
        return producerTemplate.requestBody("direct:" + route, statement, String.class);
    }

    @Path("/mimeMultipartMarshal/{fileName}/{fileContent}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String mimeMultipart(String body, @PathParam("fileName") String fileName,
            @PathParam("fileContent") String fileContent) throws Exception {

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

    @Path("/inbox")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getInboxNewMail() throws Exception {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        Exchange exchange = consumerTemplate.receive("seda:mail", 5000);

        MailMessage mailMessage = exchange.getMessage(MailMessage.class);
        AttachmentMessage attachmentMessage = exchange.getMessage(AttachmentMessage.class);
        Map<String, DataHandler> attachments = attachmentMessage.getAttachments();
        if (attachments.size() == 1) {
            Map.Entry<String, DataHandler> next = attachments.entrySet().iterator().next();
            DataHandler handler = attachments.get(next.getKey());
            builder.add("attachmentFilename", handler.getName());

            String content = context.getTypeConverter().convertTo(String.class, handler.getInputStream());
            builder.add("attachmentContent", content);
        } else {
            throw new IllegalStateException("Expected 1 attachment but got " + attachments.size());
        }

        builder.add("subject", mailMessage.getMessage().getSubject());
        builder.add("content", mailMessage.getBody(String.class));
        return builder.build();
    }

    @POST
    @Path("consumer/{enable}")
    public void manageMailConsumer(@PathParam("enable") boolean enable) throws Exception {
        if (enable) {
            context.getRouteController().startRoute("mail-consumer");
        } else {
            context.getRouteController().stopRoute("mail-consumer");
        }
    }
}
