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
package org.apache.camel.quarkus.component.arangodb.it;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.util.MapBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

import static org.apache.camel.component.arangodb.ArangoDbConstants.AQL_QUERY;
import static org.apache.camel.component.arangodb.ArangoDbConstants.AQL_QUERY_BIND_PARAMETERS;
import static org.apache.camel.component.arangodb.ArangoDbConstants.AQL_QUERY_OPTIONS;
import static org.apache.camel.component.arangodb.ArangoDbConstants.ARANGO_KEY;
import static org.apache.camel.component.arangodb.ArangoDbConstants.RESULT_CLASS_TYPE;

@Path("/arangodb/camel")
@ApplicationScoped
public class ArangodbResource {

    private static final Logger LOG = Logger.getLogger(ArangodbResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response put(String message) throws Exception {
        LOG.infof("Saving to arangodb: %s", message);
        final DocumentCreateEntity<?> response = producerTemplate.requestBody(
                "arangodb:test?host={{camel.arangodb.host}}&port={{camel.arangodb.port}}&documentCollection=camel&operation=SAVE_DOCUMENT",
                message, DocumentCreateEntity.class);
        LOG.infof("Got response from arangodb: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getKey())
                .build();
    }

    @Path("{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response get(@PathParam("key") String key) throws Exception {
        LOG.infof("Retrieve document from arangodb with key: %s", key);
        final String response = producerTemplate.requestBodyAndHeader(
                "arangodb:test?host={{camel.arangodb.host}}&port={{camel.arangodb.port}}&documentCollection=camel&operation=FIND_DOCUMENT_BY_KEY",
                key, RESULT_CLASS_TYPE, String.class, String.class);
        LOG.infof("Got response from arangodb: %s", response);
        return Response
                .ok()
                .entity(response)
                .build();
    }

    @Path("{key}")
    @DELETE
    public Response delete(@PathParam("key") String key) throws Exception {
        LOG.infof("Delete document from arangodb with key : %s", key);
        producerTemplate.requestBody(
                "arangodb:test?host={{camel.arangodb.host}}&port={{camel.arangodb.port}}&documentCollection=camel&operation=DELETE_DOCUMENT",
                key, DocumentDeleteEntity.class);

        return Response
                .ok()
                .build();
    }

    @Path("{key}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response post(@PathParam("key") String key, String msg) throws Exception {
        LOG.infof("Update document from arangodb with key : %s", key);
        Map<String, Object> headers = new HashMap<>();
        headers.put(RESULT_CLASS_TYPE, String.class);
        headers.put(ARANGO_KEY, key);

        producerTemplate.requestBodyAndHeaders(
                "arangodb:test?host={{camel.arangodb.host}}&port={{camel.arangodb.port}}&documentCollection=camel&operation=UPDATE_DOCUMENT",
                msg, headers, String.class);
        return Response
                .ok()
                .build();
    }

    @Path("/foo/{fooName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getByFooName(@PathParam("fooName") String fooName) throws Exception {
        LOG.infof("Retrieve document from arangodb with foo: %s", fooName);

        String query = "FOR t IN camel FILTER t.foo == @foo RETURN t";
        Map<String, Object> bindVars = new MapBuilder().put("foo", fooName)
                .get();

        Map<String, Object> headers = new HashMap<>();
        headers.put(AQL_QUERY, query);
        headers.put(AQL_QUERY_BIND_PARAMETERS, bindVars);
        headers.put(AQL_QUERY_OPTIONS, null);
        headers.put(RESULT_CLASS_TYPE, String.class);

        final Collection<?> responseList = producerTemplate.requestBodyAndHeaders(
                "arangodb:test?host={{camel.arangodb.host}}&port={{camel.arangodb.port}}&operation=AQL_QUERY",
                fooName, headers, Collection.class);

        return Response
                .ok()
                .entity(responseList.toString())
                .build();
    }
}
