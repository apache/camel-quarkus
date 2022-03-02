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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jvnet.mock_javamail.Mailbox;

@Path("/mock")
@ApplicationScoped
public class MockMailbox {

    @DELETE
    @Path("/clear")
    public void clearMail() {
        Mailbox.clearAll();
    }

    @GET
    @Path("/{username}/size")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSize(@PathParam("username") String username) throws Exception {
        Mailbox mailbox = Mailbox.get(username);
        return Integer.toString(mailbox.size());
    }

    @GET
    @Path("/{username}/{id}/content")
    @Produces(MediaType.TEXT_PLAIN)
    public String getContent(@PathParam("username") String username, @PathParam("id") int id) throws Exception {
        Mailbox mailbox = Mailbox.get(username);
        return mailbox.get(id).getContent().toString();
    }

    @GET
    @Path("/{username}/{id}/subject")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSubject(@PathParam("username") String username, @PathParam("id") int id) throws Exception {
        Mailbox mailbox = Mailbox.get(username);
        return mailbox.get(id).getSubject();
    }

    @POST
    @Path("/{username}/create/attachments")
    @Produces(MediaType.TEXT_PLAIN)
    public String createMailMessageWithAttachments(@PathParam("username") String username, String content) throws Exception {
        Mailbox mailbox = Mailbox.get(username);
        Message message = new MimeMessage(Session.getInstance(new Properties()));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress("camel@apache.org"));
        message.setSubject("Test attachment message");

        String attachmentContent = "Attachment " + content;
        java.nio.file.Path attachment = Files.createTempFile("cq-attachment", ".txt");
        Files.write(attachment, attachmentContent.getBytes(StandardCharsets.UTF_8));

        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(content);

        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.attachFile(attachment.toFile());

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        multipart.addBodyPart(attachmentPart);
        message.setContent(multipart);

        mailbox.add(message);

        return attachment.toAbsolutePath().toString();
    }
}
