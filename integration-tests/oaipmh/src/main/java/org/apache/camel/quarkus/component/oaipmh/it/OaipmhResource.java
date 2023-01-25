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
package org.apache.camel.quarkus.component.oaipmh.it;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.logging.Logger;

@Path("/oaipmh")
@ApplicationScoped
public class OaipmhResource {

    private static final Logger LOG = Logger.getLogger(OaipmhResource.class);

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/consumerListRecords")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> consumerListRecords() {
        LOG.debugf("Calling consumerListRecords()");
        MockEndpoint mockEndpoint = context.getEndpoint("mock:consumerListRecords", MockEndpoint.class);
        return mockEndpoint.getExchanges().stream().map(e -> e.getIn().getBody(String.class)).collect(Collectors.toList());
    }

    @Path("/consumerListRecordsParticularCase")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> consumerListRecordsParticularCase() {
        LOG.debugf("Calling consumerListRecordsParticularCase()");
        MockEndpoint mockEndpoint = context.getEndpoint("mock:consumerListRecordsParticularCase", MockEndpoint.class);
        return mockEndpoint.getExchanges().stream().map(e -> e.getIn().getBody(String.class)).collect(Collectors.toList());
    }

    @Path("/consumerIdentifyHttps")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> consumerListRecordsHttps() {
        LOG.debugf("Calling consumerIdentifyHttps()");
        MockEndpoint mockEndpoint = context.getEndpoint("mock:consumerIdentifyHttps", MockEndpoint.class);
        return mockEndpoint.getExchanges().stream().map(e -> e.getIn().getBody(String.class)).collect(Collectors.toList());
    }

    @Path("/producerListRecords")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> producerListRecords() {
        LOG.debugf("Calling producerListRecords()");
        MockEndpoint mockEndpoint = context.getEndpoint("mock:producerListRecords", MockEndpoint.class);
        producerTemplate.requestBody("direct:producerListRecords", "");
        return mockEndpoint.getExchanges().stream().map(e -> e.getIn().getBody(String.class)).collect(Collectors.toList());
    }

    @Path("/producerGetRecord")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> producerGetRecord(String oaimphIdentifier) {
        LOG.debugf("Calling producerGetRecord(%s)", oaimphIdentifier);
        MockEndpoint mockEndpoint = context.getEndpoint("mock:producerGetRecord", MockEndpoint.class);
        producerTemplate.requestBodyAndHeader("direct:producerGetRecord", "", "CamelOaimphIdentifier", oaimphIdentifier);
        return mockEndpoint.getExchanges().stream().map(e -> e.getIn().getBody(String.class)).collect(Collectors.toList());
    }
}
