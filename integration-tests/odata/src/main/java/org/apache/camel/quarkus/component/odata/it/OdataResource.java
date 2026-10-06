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
package org.apache.camel.quarkus.component.odata.it;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.odata.ODataConstants;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.util.json.JsonObject;

@Path("/odata")
@ApplicationScoped
public class OdataResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/airports")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String readAirports(@QueryParam("filter") String filter, @QueryParam("select") String select) {
        Map<String, Object> headers = new HashMap<>();
        if (filter != null) {
            headers.put(ODataConstants.FILTER, filter);
        }
        if (select != null) {
            headers.put(ODataConstants.SELECT, select);
        }
        return request("direct:readAirports", null, headers);
    }

    @Path("/people/{userName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String readPerson(@PathParam("userName") String userName) {
        return request("direct:readPerson", null, keyHeaders(userName, null));
    }

    @Path("/people")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createPerson(String person) {
        return request("direct:createPerson", person, new HashMap<>());
    }

    @Path("/people/map")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String createPersonFromMap(@QueryParam("userName") String userName, @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName) {
        Map<String, Object> person = Map.of("UserName", userName, "FirstName", firstName, "LastName", lastName);
        return request("direct:createPersonFromMap", person, new HashMap<>());
    }

    @Path("/people/{userName}")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updatePerson(@PathParam("userName") String userName, @QueryParam("etag") String etag, String person) {
        return request("direct:updatePerson", person, keyHeaders(userName, etag));
    }

    @Path("/people/{userName}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public String deletePerson(@PathParam("userName") String userName, @QueryParam("etag") String etag) {
        return request("direct:deletePerson", null, keyHeaders(userName, etag));
    }

    private static Map<String, Object> keyHeaders(String userName, String etag) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ODataConstants.KEY, "'" + userName + "'");
        if (etag != null) {
            headers.put(ODataConstants.ETAG, etag);
        }
        return headers;
    }

    private String request(String endpointUri, Object body, Map<String, Object> headers) {
        Exchange exchange = producerTemplate.request(endpointUri, e -> {
            e.getMessage().setBody(body);
            e.getMessage().setHeaders(headers);
        });

        JsonObject result = new JsonObject();
        Exception exception = exchange.getException();
        if (exception instanceof HttpOperationFailedException httpException) {
            result.put("status", httpException.getStatusCode());
        } else if (exception != null) {
            throw new RuntimeCamelException(exception);
        } else {
            Message message = exchange.getMessage();
            result.put("status", message.getHeader(Exchange.HTTP_RESPONSE_CODE));
            result.put("etag", message.getHeader(ODataConstants.ETAG));
            result.put("count", message.getHeader(ODataConstants.COUNT));
            result.put("nextLink", message.getHeader(ODataConstants.NEXT_LINK));
            result.put("body", message.getBody());
        }
        return result.toJson();
    }
}
