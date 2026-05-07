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
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Profile;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.google.mail.stream.GoogleMailStreamConstants;

import static jakarta.mail.Message.RecipientType.TO;

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

    @Path("/thread-id")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getThreadId(@QueryParam("messageId") String messageId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.id", messageId);

        try {
            Message response = producerTemplate.requestBodyAndHeaders("google-mail://messages/get", null, headers,
                    Message.class);
            if (response != null && response.getThreadId() != null) {
                return Response.ok(response.getThreadId()).build();
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

    @Path("/message-id-header")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMessageIdHeader(@QueryParam("messageId") String messageId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.id", messageId);

        try {
            Message response = producerTemplate.requestBodyAndHeaders("google-mail://messages/get", null, headers,
                    Message.class);
            if (response != null && response.getPayload() != null && response.getPayload().getHeaders() != null) {
                for (MessagePartHeader header : response.getPayload().getHeaders()) {
                    if ("Message-ID".equalsIgnoreCase(header.getName())) {
                        return Response.ok(header.getValue()).build();
                    }
                }
                return Response.status(Response.Status.NOT_FOUND).build();
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

    @Path("/update-labels")
    @PATCH
    public Response updateMessageLabels(@QueryParam("messageId") String messageId,
            @QueryParam("addLabel") String addLabel,
            @QueryParam("removeLabel") String removeLabel) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.id", messageId);

        // Use DataTypeTransformer: google-mail:update-message-labels
        // Set exchange variables for transformer to resolve label names to IDs
        producerTemplate.request("direct:update-message-labels", exchange -> {
            exchange.getMessage().setHeaders(headers);
            if (addLabel != null && !addLabel.isEmpty()) {
                exchange.setVariable("addLabels", Arrays.asList(addLabel));
            }
            if (removeLabel != null && !removeLabel.isEmpty()) {
                exchange.setVariable("removeLabels", Arrays.asList(removeLabel));
            }
        });

        return Response.ok().build();
    }

    @Path("/draft/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createDraft(String message,
            @QueryParam("threadId") String threadId,
            @QueryParam("messageId") String messageId) throws Exception {
        // Use DataTypeTransformer: google-mail:draft
        // Transformer reads String body and converts to Draft using headers
        Profile profile = producerTemplate.requestBody("google-mail://users/getProfile?inBody=userId", USER_ID, Profile.class);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        // Set headers for transformer to read (from google-mail-stream)
        headers.put(GoogleMailStreamConstants.MAIL_FROM, profile.getEmailAddress());
        headers.put(GoogleMailStreamConstants.MAIL_TO, profile.getEmailAddress());
        headers.put(GoogleMailStreamConstants.MAIL_SUBJECT, EMAIL_SUBJECT);
        if (threadId != null && !threadId.isEmpty()) {
            headers.put(GoogleMailStreamConstants.MAIL_THREAD_ID, threadId);
        }
        if (messageId != null && !messageId.isEmpty()) {
            headers.put(GoogleMailStreamConstants.MAIL_MESSAGE_ID, messageId);
        }

        Draft response = producerTemplate.requestBodyAndHeaders("direct:create-draft", message, headers, Draft.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getId())
                .build();
    }

    @Path("/draft/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response readDraft(@QueryParam("draftId") String draftId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.id", draftId);

        try {
            Draft response = producerTemplate.requestBodyAndHeaders("google-mail://drafts/get", null, headers, Draft.class);
            if (response != null && response.getMessage() != null && response.getMessage().getPayload() != null) {
                String body = new String(response.getMessage().getPayload().getBody().decodeData(),
                        StandardCharsets.UTF_8);
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

    @Path("/draft/update")
    @PATCH
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateDraft(@QueryParam("draftId") String draftId, String message) throws Exception {
        // Use DataTypeTransformer: google-mail:draft for update
        Profile profile = producerTemplate.requestBody("google-mail://users/getProfile?inBody=userId", USER_ID, Profile.class);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.id", draftId);
        // Set headers for transformer to read (from google-mail-stream)
        headers.put(GoogleMailStreamConstants.MAIL_FROM, profile.getEmailAddress());
        headers.put(GoogleMailStreamConstants.MAIL_TO, profile.getEmailAddress());
        headers.put(GoogleMailStreamConstants.MAIL_SUBJECT, EMAIL_SUBJECT);

        Draft response = producerTemplate.requestBodyAndHeaders("direct:update-draft", message, headers, Draft.class);
        return Response.ok(response.getId()).build();
    }

    @Path("/draft/send")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendDraft(@QueryParam("draftId") String draftId) {
        Draft draft = new Draft();
        draft.setId(draftId);

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.content", draft);

        Message response = producerTemplate.requestBodyAndHeaders("google-mail://drafts/send", null, headers, Message.class);
        return Response.ok(response.getId()).build();
    }

    @Path("/draft/delete")
    @DELETE
    public Response deleteDraft(@QueryParam("draftId") String draftId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.id", draftId);
        producerTemplate.requestBodyAndHeaders("google-mail://drafts/delete", null, headers);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Path("/label/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createLabel(String labelName) {
        Label label = new Label();
        label.setName(labelName);
        label.setLabelListVisibility("labelShow");
        label.setMessageListVisibility("show");

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.content", label);

        Label response = producerTemplate.requestBodyAndHeaders("google-mail://labels/create", null, headers, Label.class);
        return Response
                .status(Response.Status.CREATED)
                .entity(response.getId())
                .build();
    }

    @Path("/label/delete")
    @DELETE
    public Response deleteLabel(@QueryParam("labelId") String labelId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleMail.userId", USER_ID);
        headers.put("CamelGoogleMail.id", labelId);
        producerTemplate.requestBodyAndHeaders("google-mail://labels/delete", null, headers);
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
