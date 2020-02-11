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
package org.apache.camel.quarkus.component.olingo4.it;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.olingo.client.api.communication.ODataClientErrorException;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;

@Path("/olingo4")
@ApplicationScoped
public class Olingo4Resource {

    public static final String TEST_SERVICE_BASE_URL = "https://services.odata.org/TripPinRESTierService";

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/create")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response create(@QueryParam("sessionId") String sessionId, String json) throws Exception {
        ClientEntity entity = producerTemplate.requestBody(
                "olingo4://create/People?contentType=application/json;charset=utf-8&serviceUri=" + getServiceURL(sessionId),
                json, ClientEntity.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(entity)
                .build();
    }

    @Path("/read")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response read(@QueryParam("sessionId") String sessionId) {
        Response.ResponseBuilder responseBuilder = Response.status(200);
        try {
            ClientEntity entity = producerTemplate.requestBody(
                    "olingo4://read/People('lewisblack')?serviceUri=" + getServiceURL(sessionId), null, ClientEntity.class);

            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            String[] fields = new String[] { "FirstName", "LastName", "UserName", "MiddleName" };
            for (String field : fields) {
                ClientProperty property = entity.getProperty(field);
                if (property != null) {
                    objectBuilder.add(field, property.getPrimitiveValue().toString());
                }
            }
            responseBuilder.entity(objectBuilder.build());
        } catch (CamelExecutionException cee) {
            Exception exception = cee.getExchange().getException();
            if (exception instanceof ODataClientErrorException) {
                ODataClientErrorException ex = (ODataClientErrorException) exception;
                responseBuilder.status(ex.getStatusLine().getStatusCode());
            } else {
                throw cee;
            }
        }
        return responseBuilder.build();
    }

    @Path("/update")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@QueryParam("sessionId") String sessionId, String json) {
        HttpStatusCode status = producerTemplate.requestBody(
                "olingo4://update/People('lewisblack')?serviceUri=" + getServiceURL(sessionId), json, HttpStatusCode.class);
        return Response
                .status(status.getStatusCode())
                .build();
    }

    @Path("/delete")
    @DELETE
    public Response delete(@QueryParam("sessionId") String sessionId) {
        HttpStatusCode status = producerTemplate.requestBody(
                "olingo4://delete/People('lewisblack')?serviceUri=" + getServiceURL(sessionId), null, HttpStatusCode.class);
        return Response
                .status(status.getStatusCode())
                .build();
    }

    private String getServiceURL(String sessionId) {
        return String.format("%s/%s/", TEST_SERVICE_BASE_URL, sessionId);
    }
}
