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
package org.apache.camel.quarkus.component.couchbase.it;

import com.couchbase.client.java.kv.GetResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.couchbase.CouchbaseConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import static org.apache.camel.component.couchbase.CouchbaseConstants.COUCHBASE_DELETE;
import static org.apache.camel.component.couchbase.CouchbaseConstants.COUCHBASE_GET;

@Path("/couchbase")
@ApplicationScoped
@Consumes(MediaType.TEXT_PLAIN)
public class CouchbaseResource {

    private static final Logger LOG = Logger.getLogger(CouchbaseResource.class);

    @Inject
    ProducerTemplate producerTemplate;
    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = "couchbase.connection.uri")
    String connectionUri;
    @ConfigProperty(name = "couchbase.bucket.name")
    String bucketName;
    @ConfigProperty(name = "couchbase.timeout", defaultValue = "120000")
    long timeout;

    @PUT
    @Path("id/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean insert(@PathParam("id") String id, String msg) {
        LOG.infof("inserting message %s with id %s", msg, id);
        String endpoint = String.format("%s&queryTimeout=%s", connectionUri, timeout);
        return producerTemplate.requestBodyAndHeader(endpoint, msg, CouchbaseConstants.HEADER_ID, id, Boolean.class);
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getById(@PathParam("id") String id) {
        LOG.infof("Getting object with id : %s", id);
        String endpoint = String.format("%s&operation=%s&queryTimeout=%s", connectionUri, COUCHBASE_GET, timeout);
        GetResult result = producerTemplate.requestBodyAndHeader(endpoint, null, CouchbaseConstants.HEADER_ID, id,
                GetResult.class);
        return result != null ? result.contentAs(String.class) : null;
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean delete(@PathParam("id") String id) {
        LOG.infof("Deleting object with id : %s", id);
        String endpoint = String.format("%s&operation=%s&queryTimeout=%s", connectionUri, COUCHBASE_DELETE, timeout);
        producerTemplate.sendBodyAndHeader(endpoint, null, CouchbaseConstants.HEADER_ID, id);
        return true;
    }

    @GET
    @Path("poll")
    @Produces(MediaType.TEXT_PLAIN)
    public String poll() {
        LOG.infof("polling one document");
        String endpoint = String.format("%s&designDocumentName=%s&viewName=%s&limit=1", connectionUri, bucketName, bucketName);
        GetResult result = consumerTemplate.receiveBody(endpoint, timeout, GetResult.class);
        return result != null ? result.contentAs(String.class) : null;
    }
}
