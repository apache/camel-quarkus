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
package org.apache.camel.quarkus.component.hazelcast.it;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;

import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_SEDA_FIFO;
import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_SEDA_IN_ONLY;
import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_SEDA_IN_OUT;
import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_SEDA_IN_OUT_TRANSACTED;

@Path("/hazelcast/seda")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HazelcastSedaResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Inject
    @Named("hazelcastResults")
    Map<String, List<String>> hazelcastResults;

    @PUT
    @Path("fifo")
    public Response addFifo(String value) {
        producerTemplate.sendBody("hazelcast-seda:foo-fifo", value);
        return Response.accepted().build();
    }

    @GET
    @Path("fifo")
    public List<String> getFifoValues() {
        return getValues(MOCK_SEDA_FIFO);
    }

    @PUT
    @Path("in")
    public Response addInOnly(String value) {
        producerTemplate.sendBody("hazelcast-seda:foo-in-only", ExchangePattern.InOnly, value);
        return Response.accepted().build();
    }

    @GET
    @Path("in")
    public List<String> getInOnlyValues() {
        return getValues(MOCK_SEDA_IN_ONLY);
    }

    @PUT
    @Path("out")
    public Response addInOut(String value) {
        producerTemplate.sendBody("hazelcast-seda:foo-in-out", ExchangePattern.InOut, value);
        return Response.accepted().build();
    }

    @GET
    @Path("out")
    public List<String> getInOutValues() {
        return getValues(MOCK_SEDA_IN_OUT);
    }

    @PUT
    @Path("out/transacted")
    public Response addInOutTransacted(String value) {
        producerTemplate.sendBody("hazelcast-seda:foo-in-out-trans", ExchangePattern.InOut, value);
        return Response.accepted().build();
    }

    @GET
    @Path("out/transacted")
    public List<String> getInOutTransactedValues() {
        return getValues(MOCK_SEDA_IN_OUT_TRANSACTED);
    }

    private List<String> getValues(String endpoint) {
        return hazelcastResults.get(endpoint);
    }
}
