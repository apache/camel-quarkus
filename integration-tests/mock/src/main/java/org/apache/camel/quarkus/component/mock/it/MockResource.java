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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.reifier.RouteReifier;
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
        RouteReifier.adviceWith(context.adapt(ModelCamelContext.class).getRouteDefinitions().get(0), context,
                new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        // mock all endpoints
                        mockEndpoints();
                    }
                });

        MockEndpoint mockEndpoint1 = context.getEndpoint("mock:direct:start", MockEndpoint.class);
        mockEndpoint1.expectedBodiesReceived("Hello World");
        MockEndpoint mockEndpoint2 = context.getEndpoint("mock:direct:foo", MockEndpoint.class);
        mockEndpoint2.expectedBodiesReceived("Hello World");
        MockEndpoint mockEndpoint3 = context.getEndpoint("mock:log:foo", MockEndpoint.class);
        mockEndpoint3.expectedBodiesReceived("Bye World");
        MockEndpoint mockEndpoint4 = context.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint4.expectedBodiesReceived("Bye World");

        producerTemplate.sendBody("direct:start", "Hello World");

        mockEndpoint1.assertIsSatisfied();
        mockEndpoint2.assertIsSatisfied();
        mockEndpoint3.assertIsSatisfied();
        mockEndpoint4.assertIsSatisfied();

        // test to ensure correct endpoints in registry
        Assert.assertNotNull(context.hasEndpoint("direct:start"));
        Assert.assertNotNull(context.hasEndpoint("direct:foo"));
        Assert.assertNotNull(context.hasEndpoint("log:foo"));
        Assert.assertNotNull(context.hasEndpoint("mock:result"));
        // all the endpoints was mocked
        Assert.assertNotNull(context.hasEndpoint("mock:direct:start"));
        Assert.assertNotNull(context.hasEndpoint("mock:direct:foo"));
        Assert.assertNotNull(context.hasEndpoint("mock:log:foo"));
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
}
