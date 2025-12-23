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
package org.apache.camel.quarkus.component.keycloak.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.component.keycloak.KeycloakConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Keycloak REST resource for Consumer operations (event monitoring).
 */
@Path("/keycloak")
@ApplicationScoped
public class KeycloakConsumerResource extends KeycloakResourceSupport {

    @Path("/events/admin/{realmName}/enable")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response enableAdminEvents(@PathParam("realmName") String realmName) {
        try {

            Map<String, Object> headers = new HashMap<>();
            headers.put(KeycloakConstants.REALM_NAME, realmName);

            RealmRepresentation realm = producerTemplate.requestBodyAndHeaders(
                    getKeycloakEndpoint() + "&operation=getRealm",
                    null,
                    headers,
                    RealmRepresentation.class);

            // Enable admin events
            realm.setAdminEventsEnabled(true);
            realm.setAdminEventsDetailsEnabled(true);
            realm.setEventsEnabled(true);

            producerTemplate.requestBodyAndHeaders(
                    getKeycloakEndpoint() + "&operation=updateRealm&pojoRequest=true",
                    realm,
                    headers,
                    String.class);

            return Response.ok("Admin events enabled successfully").build();
        } catch (Exception e) {
            LOG.error("Failed to enable admin events", e);
            return Response.status(500).entity("Failed to enable admin events: " + e.getMessage()).build();
        }
    }

    @Path("/events/admin/collected")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCollectedAdminEvents() {
        try {
            MockEndpoint mockEndpoint = context.getEndpoint("mock:admin-events", MockEndpoint.class);
            List<AdminEventRepresentation> events = new ArrayList<>();

            mockEndpoint.getExchanges().forEach(exchange -> {
                Object body = exchange.getIn().getBody();
                if (body instanceof AdminEventRepresentation) {
                    events.add((AdminEventRepresentation) body);
                }
            });

            return Response.ok(events).build();
        } catch (Exception e) {
            LOG.error("Failed to get collected admin events", e);
            return Response.status(500).entity("Failed to get admin events: " + e.getMessage()).build();
        }
    }

    @Path("/events/regular/collected")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCollectedRegularEvents() {
        try {
            MockEndpoint mockEndpoint = context.getEndpoint("mock:events", MockEndpoint.class);
            List<EventRepresentation> events = new ArrayList<>();

            mockEndpoint.getExchanges().forEach(exchange -> {
                Object body = exchange.getIn().getBody();
                if (body instanceof EventRepresentation) {
                    events.add((EventRepresentation) body);
                }
            });

            return Response.ok(events).build();
        } catch (Exception e) {
            LOG.error("Failed to get collected regular events", e);
            return Response.status(500).entity("Failed to get regular events: " + e.getMessage()).build();
        }
    }

    @Path("/events/admin/reset")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response resetAdminEventsMock() {
        try {
            MockEndpoint mockEndpoint = context.getEndpoint("mock:admin-events", MockEndpoint.class);
            mockEndpoint.reset();
            return Response.ok("Admin events mock reset successfully").build();
        } catch (Exception e) {
            LOG.error("Failed to reset admin events mock", e);
            return Response.status(500).entity("Failed to reset mock: " + e.getMessage()).build();
        }
    }

    @Path("/events/regular/reset")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response resetRegularEventsMock() {
        try {
            MockEndpoint mockEndpoint = context.getEndpoint("mock:events", MockEndpoint.class);
            mockEndpoint.reset();
            return Response.ok("Regular events mock reset successfully").build();
        } catch (Exception e) {
            LOG.error("Failed to reset regular events mock", e);
            return Response.status(500).entity("Failed to reset mock: " + e.getMessage()).build();
        }
    }

    @Path("/consumer/admin-events/start/{realmName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response startAdminEventsConsumer(@PathParam("realmName") String realmName) {
        try {
            context.getRouteController().startRoute("admin-events-consumer-" + realmName);
            return Response.ok("Admin events consumer started").build();
        } catch (Exception e) {
            LOG.error("Failed to start admin events consumer", e);
            return Response.status(500).entity("Failed to start consumer: " + e.getMessage()).build();
        }
    }

    @Path("/consumer/regular-events/start/{realmName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response startRegularEventsConsumer(@PathParam("realmName") String realmName) {
        try {
            context.getRouteController().startRoute("regular-events-consumer-" + realmName);
            return Response.ok("Regular events consumer started").build();
        } catch (Exception e) {
            LOG.error("Failed to start regular events consumer", e);
            return Response.status(500).entity("Failed to start consumer: " + e.getMessage()).build();
        }
    }

    @Path("/consumer/route/create/{realmName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createConsumerRoutes(@PathParam("realmName") String realmName) {
        try {
            // Create admin events consumer route
            context.addRoutes(new org.apache.camel.builder.RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("keycloak:adminEvents"
                            + "?serverUrl=" + keycloakUrl
                            + "&authRealm=" + keycloakRealm
                            + "&username=" + keycloakUsername
                            + "&password=" + keycloakPassword
                            + "&realm=" + realmName
                            + "&eventType=admin-events"
                            + "&maxResults=50"
                            + "&initialDelay=500"
                            + "&delay=1000")
                            .autoStartup(false)
                            .routeId("admin-events-consumer-" + realmName)
                            .to("mock:admin-events");

                    // Create regular events consumer route
                    from("keycloak:events"
                            + "?serverUrl=" + keycloakUrl
                            + "&authRealm=" + keycloakRealm
                            + "&username=" + keycloakUsername
                            + "&password=" + keycloakPassword
                            + "&realm=" + realmName
                            + "&eventType=events"
                            + "&maxResults=50"
                            + "&initialDelay=500"
                            + "&delay=1000")
                            .autoStartup(false)
                            .routeId("regular-events-consumer-" + realmName)
                            .to("mock:events");
                }
            });

            return Response.ok("Consumer routes created successfully").build();
        } catch (Exception e) {
            LOG.error("Failed to create consumer routes", e);
            return Response.status(500).entity("Failed to create routes: " + e.getMessage()).build();
        }
    }
}
