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
package org.apache.camel.quarkus.component.mock.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.jboss.logging.Logger;
import org.wildfly.common.Assert;

@Path("/mock")
@ApplicationScoped
public class MockResource {

    private static final Logger LOG = Logger.getLogger(MockResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/advice")
    @GET
    public void advice() throws Exception {

        // advice the first route using the inlined AdviceWith route builder
        // which has extended capabilities than the regular route builder
        AdviceWith.adviceWith(((ModelCamelContext) context).getRouteDefinition("forMocking"), context,
                new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        mockEndpoints("direct:mock.*", "log:mock.*");
                    }
                });

        MockEndpoint mockEndpoint1 = context.getEndpoint("mock:direct:mockStart", MockEndpoint.class);
        mockEndpoint1.expectedBodiesReceived("Hello World");
        MockEndpoint mockEndpoint2 = context.getEndpoint("mock:direct:mockFoo", MockEndpoint.class);
        mockEndpoint2.expectedBodiesReceived("Hello World");
        MockEndpoint mockEndpoint3 = context.getEndpoint("mock:log:mockFoo", MockEndpoint.class);
        mockEndpoint3.expectedBodiesReceived("Bye World");
        MockEndpoint mockEndpoint4 = context.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint4.expectedBodiesReceived("Bye World");

        producerTemplate.sendBody("direct:mockStart", "Hello World");

        mockEndpoint1.assertIsSatisfied();
        mockEndpoint2.assertIsSatisfied();
        mockEndpoint3.assertIsSatisfied();
        mockEndpoint4.assertIsSatisfied();

        // test to ensure correct endpoints in registry
        Assert.assertNotNull(context.hasEndpoint("direct:mockStart"));
        Assert.assertNotNull(context.hasEndpoint("direct:mockFoo"));
        Assert.assertNotNull(context.hasEndpoint("log:mockFoo"));
        Assert.assertNotNull(context.hasEndpoint("mock:result"));
        // all the endpoints was mocked
        Assert.assertNotNull(context.hasEndpoint("mock:direct:mockStart"));
        Assert.assertNotNull(context.hasEndpoint("mock:direct:mockFoo"));
        Assert.assertNotNull(context.hasEndpoint("mock:log:mockFoo"));
    }

    @Path("/basic")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void post(String message) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:foo", MockEndpoint.class);
        mockEndpoint.expectedBodyReceived().constant(message);
        mockEndpoint.setExpectedCount(1);
        LOG.infof("Sending to mock: %s", message);
        final String response = producerTemplate.requestBody("mock:foo", message, String.class);
        LOG.infof("Got response from mock: %s", response);

        mockEndpoint.assertIsSatisfied();
    }

    @Path("/route/{route}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public String post(String message, @PathParam("route") String route) throws Exception {
        return producerTemplate.requestBody("direct:" + route, message, String.class);
    }

}
