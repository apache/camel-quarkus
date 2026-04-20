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
package org.apache.camel.quarkus.component.ibm.watson.discovery.it;

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
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/ibm-watson-discovery")
@ApplicationScoped
public class IbmWatsonDiscoveryResource {

    private static final Logger LOG = Logger.getLogger(IbmWatsonDiscoveryResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/collections")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCollections() {
        Exchange exchange = producerTemplate.request("direct:list-collections", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
            }
        });

        if (exchange.getException() != null) {
            LOG.errorf("Error listing collections: %s", exchange.getException().getMessage());
            return Response.serverError().entity(exchange.getException().getMessage()).build();
        }

        Object body = exchange.getMessage().getBody();
        String jsonResponse = body != null ? body.toString() : "{}";
        return Response.ok(jsonResponse).build();
    }

    @Path("/collections/{name}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCollection(@PathParam("name") String name) {
        Exchange exchange = producerTemplate.request("direct:create-collection", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setHeader("CamelIBMWatsonDiscoveryCollectionName", name);
                exchange.getMessage().setHeader("CamelIBMWatsonDiscoveryCollectionDescription",
                        "Test collection created by Camel Quarkus");
            }
        });

        if (exchange.getException() != null) {
            LOG.errorf("Error creating collection: %s", exchange.getException().getMessage());
            return Response.serverError().entity(exchange.getException().getMessage()).build();
        }

        Object body = exchange.getMessage().getBody();
        String jsonResponse = body != null ? body.toString() : "{}";

        return Response.ok(jsonResponse).build();
    }

    @Path("/collections/{collectionId}")
    @DELETE
    public Response deleteCollection(@PathParam("collectionId") String collectionId) {
        Exchange exchange = producerTemplate.request("direct:delete-collection", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setHeader("CamelIBMWatsonDiscoveryCollectionId", collectionId);
            }
        });

        if (exchange.getException() != null) {
            LOG.errorf("Error deleting collection: %s", exchange.getException().getMessage());
            return Response.serverError().entity(exchange.getException().getMessage()).build();
        }

        return Response.noContent().build();
    }

    @Path("/collections/{collectionId}/documents")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addDocument(@PathParam("collectionId") String collectionId, String document) {
        Exchange exchange = producerTemplate.request("direct:add-document", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setHeader("CamelIBMWatsonDiscoveryCollectionId", collectionId);
                exchange.getMessage().setBody(document);
            }
        });

        if (exchange.getException() != null) {
            LOG.errorf("Error adding document: %s", exchange.getException().getMessage());
            return Response.serverError().entity(exchange.getException().getMessage()).build();
        }

        Object body = exchange.getMessage().getBody();
        String jsonResponse = body != null ? body.toString() : "{}";
        return Response.ok(jsonResponse).build();
    }

    @Path("/collections/{collectionId}/documents/{documentId}")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDocument(@PathParam("collectionId") String collectionId,
            @PathParam("documentId") String documentId, String document) {
        Exchange exchange = producerTemplate.request("direct:update-document", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setHeader("CamelIBMWatsonDiscoveryCollectionId", collectionId);
                exchange.getMessage().setHeader("CamelIBMWatsonDiscoveryDocumentId", documentId);
                exchange.getMessage().setBody(document);
            }
        });

        if (exchange.getException() != null) {
            LOG.errorf("Error updating document: %s", exchange.getException().getMessage());
            return Response.serverError().entity(exchange.getException().getMessage()).build();
        }

        Object body = exchange.getMessage().getBody();
        String jsonResponse = body != null ? body.toString() : "{}";
        return Response.ok(jsonResponse).build();
    }

    @Path("/collections/{collectionId}/documents/{documentId}")
    @DELETE
    public Response deleteDocument(@PathParam("collectionId") String collectionId,
            @PathParam("documentId") String documentId) {
        Exchange exchange = producerTemplate.request("direct:delete-document", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setHeader("CamelIBMWatsonDiscoveryCollectionId", collectionId);
                exchange.getMessage().setHeader("CamelIBMWatsonDiscoveryDocumentId", documentId);
            }
        });

        if (exchange.getException() != null) {
            LOG.errorf("Error deleting document: %s", exchange.getException().getMessage());
            return Response.serverError().entity(exchange.getException().getMessage()).build();
        }

        return Response.noContent().build();
    }

    @Path("/collections/{collectionId}/query")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(@PathParam("collectionId") String collectionId, String queryText) {
        Exchange exchange = producerTemplate.request("direct:query", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setHeader("CamelIBMWatsonDiscoveryCollectionId", collectionId);
                exchange.getMessage().setHeader("CamelIBMWatsonDiscoveryQuery", queryText);
            }
        });

        if (exchange.getException() != null) {
            LOG.errorf("Error querying: %s", exchange.getException().getMessage());
            return Response.serverError().entity(exchange.getException().getMessage()).build();
        }

        Object body = exchange.getMessage().getBody();
        String jsonResponse = body != null ? body.toString() : "{}";
        return Response.ok(jsonResponse).build();
    }
}
