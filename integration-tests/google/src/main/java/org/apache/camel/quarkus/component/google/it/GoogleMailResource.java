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
package org.apache.camel.quarkus.component.google.it;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Profile;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;

import static javax.mail.Message.RecipientType.TO;

@Path("/google-mail")
public class GoogleMailResource {

    private static final String EMAIL_SUBJECT = "Camel Quarkus Google Mail Test";
    private static final String USER_ID = "me";

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createMail(String message) throws Exception {
        Message email = createMessage(message);
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.content", email);
        final Message response = producerTemplate.requestBodyAndHeaders("google-mail://messages/send", null, headers,
                Message.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getId())
                .build();
    }

    @Path("/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response readMail(@QueryParam("messageId") String messageId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.id", messageId);

        try {
            Message response = producerTemplate.requestBodyAndHeaders("google-mail://messages/get", null, headers,
                    Message.class);
            if (response != null && response.getPayload() != null) {
                String body = new String(response.getPayload().getBody().decodeData(), StandardCharsets.UTF_8);
                return Response.ok(body).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (CamelExecutionException e) {
            Exception exchangeException = e.getExchange().getException();
            if (exchangeException != null && exchangeException.getCause() instanceof GoogleJsonResponseException) {
                GoogleJsonResponseException originalException = (GoogleJsonResponseException) exchangeException.getCause();
                return Response.status(originalException.getStatusCode()).build();
            }
            throw e;
        }
    }

    @Path("/delete")
    @DELETE
    public Response deleteMail(@QueryParam("messageId") String messageId, String message) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.id", messageId);
        producerTemplate.requestBodyAndHeaders("google-mail://messages/delete", message, headers);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private Message createMessage(String body) throws MessagingException, IOException {
        Session session = Session.getDefaultInstance(new Properties(), null);

        Profile profile = producerTemplate.requestBody("google-mail://users/getProfile?inBody=userId", USER_ID, Profile.class);
        MimeMessage mm = new MimeMessage(session);
        mm.addRecipients(TO, profile.getEmailAddress());
        mm.setSubject(EMAIL_SUBJECT);
        mm.setContent(body, "text/plain");

        Message message = new Message();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            mm.writeTo(baos);
            message.setRaw(Base64.getUrlEncoder().encodeToString(baos.toByteArray()));
        }
        return message;
    }
}
