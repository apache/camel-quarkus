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
package org.apache.camel.quarkus.component.couchdb.it;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.couchdb.CouchDbConstants;
import org.apache.camel.component.couchdb.CouchDbOperations;
import org.jboss.logging.Logger;
import org.lightcouch.NoDocumentException;

import static org.apache.camel.quarkus.component.couchdb.it.CouchDbRoute.COUCHDB_ENDPOINT_URI;

@Path("/couchdb")
@ApplicationScoped
public class CouchdbResource {

    private static final Logger LOG = Logger.getLogger(CouchdbResource.class);

    private final ConcurrentLinkedQueue<CouchdbTestDocument> loggedEvents = new ConcurrentLinkedQueue<>();

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/create")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CouchdbTestDocument create(CouchdbTestDocument document) {
        LOG.info("Invoking create");
        Exchange createExchange = producerTemplate.request(COUCHDB_ENDPOINT_URI,
                e -> e.getMessage().setBody(document.toJsonObject()));
        document.setId(createExchange.getMessage().getHeader(CouchDbConstants.HEADER_DOC_ID, String.class));
        document.setRevision(createExchange.getMessage().getHeader(CouchDbConstants.HEADER_DOC_REV, String.class));
        return document;
    }

    @Path("/get")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String get(CouchdbTestDocument document) {
        LOG.info("Invoking get");
        Exchange getExchange = producerTemplate.request(COUCHDB_ENDPOINT_URI, e -> {
            e.getMessage().setBody(document.toJsonObject());
            e.getMessage().setHeader(CouchDbConstants.HEADER_METHOD, CouchDbOperations.GET);
            e.getMessage().setHeader(CouchDbConstants.HEADER_DOC_ID, document.getId());
        });
        if (getExchange.getException(NoDocumentException.class) != null) {
            return null;
        } else {
            return getExchange.getMessage().getBody(String.class);
        }
    }

    @Path("/update")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CouchdbTestDocument update(CouchdbTestDocument document) {
        LOG.info("Invoking update");
        Exchange updateExchange = producerTemplate.request(COUCHDB_ENDPOINT_URI, e -> {
            e.getMessage().setBody(document.toJsonObject());
        });
        document.setId(updateExchange.getMessage().getHeader(CouchDbConstants.HEADER_DOC_ID, String.class));
        document.setRevision(updateExchange.getMessage().getHeader(CouchDbConstants.HEADER_DOC_REV, String.class));
        return document;
    }

    @Path("/delete")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CouchdbTestDocument delete(CouchdbTestDocument document) {
        LOG.info("Invoking delete");
        Exchange deleteExchange = producerTemplate.request(COUCHDB_ENDPOINT_URI, e -> {
            e.getMessage().setBody(document.toJsonObject());
            e.getMessage().setHeader(CouchDbConstants.HEADER_METHOD, CouchDbOperations.DELETE);
        });
        document.setId(deleteExchange.getMessage().getHeader(CouchDbConstants.HEADER_DOC_ID, String.class));
        document.setRevision(deleteExchange.getMessage().getHeader(CouchDbConstants.HEADER_DOC_REV, String.class));
        return document;
    }

    void logEvent(CouchdbTestDocument event) {
        loggedEvents.add(event);
    }

    @Path("/get-events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<CouchdbTestDocument> getEvents() {
        return loggedEvents;
    }
}
