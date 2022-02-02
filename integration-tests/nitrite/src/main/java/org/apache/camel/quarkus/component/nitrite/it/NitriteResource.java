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
package org.apache.camel.quarkus.component.nitrite.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.Message;
import org.apache.camel.component.nitrite.NitriteConstants;
import org.dizitart.no2.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/nitrite")
@ApplicationScoped
public class NitriteResource {
    private static final Logger LOG = Logger.getLogger(NitriteResource.class);

    public static final String PROPERTY_DB_FILE = "camel.quarkus.nitrite.test.db.file";

    @ConfigProperty(name = PROPERTY_DB_FILE)
    String dbFile;

    @Inject
    FluentProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/getRepositoryClass")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRepositoryClass(@QueryParam("mappable") boolean mappable) throws Exception {
        String className = mappable ? EmployeeMappable.class.getName() : EmployeeSerializable.class.getName();
        final Exchange exchange = consumerTemplate.receive(String.format("nitrite://%s?repositoryClass=%s",
                dbFile, className), 2000);
        if (exchange == null) {
            return Response.noContent().build();
        }
        final Message message = exchange.getMessage();
        return Response
                .ok(message.getBody())
                .header(NitriteConstants.CHANGE_TYPE, message.getHeader(NitriteConstants.CHANGE_TYPE))
                .build();
    }

    @Path("/repositoryClass")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object postRepositoryClass(EmployeeSerializable object, @QueryParam("mappable") boolean mappable) {
        String className = mappable ? EmployeeMappable.class.getName() : EmployeeSerializable.class.getName();
        //if object, is mappable, construct it from serializable (it is conversion caused by the type in method parameter)
        Employee employee = object;
        if (mappable) {
            employee = new EmployeeMappable(object);
        }
        LOG.debugf("Sending to nitrite: {%s}", object);
        return producerTemplate.toF("nitrite://%s?repositoryClass=%s",
                dbFile, className)
                .withBody(employee)
                .withHeader(NitriteConstants.OPERATION, null)
                .request();
    }

    @Path("/repositoryClassOperation")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object postRepositoryClassOperation(Operation operation, @QueryParam("mappable") boolean mappable) {
        String className = mappable ? EmployeeMappable.class.getName() : EmployeeSerializable.class.getName();
        LOG.debugf("Sending to nitrite: {%s}", operation);
        return producerTemplate.toF("nitrite://%s?repositoryClass=%s",
                dbFile, className)
                .withBody(mappable ? operation.getEmployeeMappable() : operation.getEmployeeSerializable())
                .withHeader(NitriteConstants.OPERATION, operation.toRepositoryOperation())
                .request();
    }

    @Path("/collection")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Object collection() throws Exception {
        final Exchange exchange = consumerTemplate.receive(String.format("nitrite://%s?collection=collection",
                dbFile), 5000);
        LOG.debugf("Received from nitrite: %s", exchange == null ? null : exchange.getIn().getBody());
        if (exchange == null) {
            return Response.noContent().build();
        }
        final Message message = exchange.getMessage();
        return Response
                .ok(message.getBody())
                .header(NitriteConstants.CHANGE_TYPE, message.getHeader(NitriteConstants.CHANGE_TYPE))
                .build();
    }

    @Path("/collection")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object collection(Document doc) {
        LOG.debugf("Sending to nitrite: {%s}", doc);
        return producerTemplate.toF("nitrite://%s?collection=collection", dbFile)
                .withBody(doc)
                .withHeader(NitriteConstants.OPERATION, null)
                .request();
    }

    @Path("/collectionOperation")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object collectionOperation(Operation operation) {
        LOG.debugf("Sending to nitrite: {%s}", operation);
        return producerTemplate.toF("nitrite://%s?collection=collection",
                dbFile)
                .withBody(operation.getDocument())
                .withHeader(NitriteConstants.OPERATION, operation.toCollectionOperation())
                .request();
    }
}
