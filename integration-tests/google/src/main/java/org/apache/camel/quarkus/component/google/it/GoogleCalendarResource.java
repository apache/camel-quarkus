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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.Event;
import jakarta.inject.Inject;
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

@Path("/google-calendar")
public class GoogleCalendarResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createCalendar(String summary) throws Exception {
        Calendar calendar = new Calendar();
        calendar.setSummary(summary);
        calendar.setTimeZone("Europe/London");
        Calendar response = producerTemplate.requestBody("google-calendar://calendars/insert?inBody=content", calendar,
                Calendar.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getId())
                .build();
    }

    @Path("/create/event")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createCalendarEvent(@QueryParam("calendarId") String calendarId, String eventText) throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleCalendar.calendarId", calendarId);
        headers.put("CamelGoogleCalendar.text", eventText);
        Event response = producerTemplate.requestBodyAndHeaders("google-calendar://events/quickAdd", null, headers,
                Event.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getId())
                .build();
    }

    @Path("/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response readCalendar(@QueryParam("calendarId") String calendarId) {
        try {
            Calendar response = producerTemplate.requestBody("google-calendar://calendars/get?inBody=calendarId", calendarId,
                    Calendar.class);
            if (response != null) {
                return Response.ok(response.getSummary()).build();
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

    @Path("/read/event")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response readCalendarEvent(@QueryParam("calendarId") String calendarId, @QueryParam("eventId") String eventId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleCalendar.calendarId", calendarId);
        headers.put("CamelGoogleCalendar.eventId", eventId);
        try {
            Event response = producerTemplate.requestBodyAndHeaders("google-calendar://events/get", null, headers, Event.class);
            if (response != null) {
                return Response.ok(response.getSummary()).build();
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

    @Path("/update/event")
    @PATCH
    public Response updateCalendarEvent(@QueryParam("calendarId") String calendarId, @QueryParam("eventId") String eventId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleCalendar.calendarId", calendarId);
        headers.put("CamelGoogleCalendar.eventId", eventId);
        try {
            Event response = producerTemplate.requestBodyAndHeaders("google-calendar://events/get", null, headers,
                    Event.class);
            if (response == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            response.setSummary(response.getSummary() + " Updated");

            headers = new HashMap<>();
            headers.put("CamelGoogleCalendar.calendarId", calendarId);
            headers.put("CamelGoogleCalendar.eventId", eventId);
            headers.put("CamelGoogleCalendar.content", response);
            producerTemplate.requestBodyAndHeaders("google-calendar://events/update", null, headers);
            return Response.ok().build();
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
    public Response deleteCalendar(@QueryParam("calendarId") String calendarId) {
        producerTemplate.requestBody("google-calendar://calendars/delete?inBody=calendarId", calendarId);
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
