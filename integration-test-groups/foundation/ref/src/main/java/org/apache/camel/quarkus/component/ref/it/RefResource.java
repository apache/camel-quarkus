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
package org.apache.camel.quarkus.component.ref.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.ExpressionAdapter;

@Path("/ref")
@ApplicationScoped
public class RefResource {
    @Inject
    CamelContext camelContext;
    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void produceMessage(@QueryParam("uri") String uri, String message) {
        producerTemplate.sendBody(uri, message);
    }

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String consumeMessage(@QueryParam("uri") String uri) {
        return consumerTemplate.receiveBody(uri, 5000L, String.class);
    }

    @Singleton
    @Named("direct-start-a")
    @javax.enterprise.inject.Produces
    public Endpoint directStartA() {
        return camelContext.getEndpoint("direct:startA");
    }

    @Singleton
    @Named("direct-start-b")
    @javax.enterprise.inject.Produces
    public Endpoint directStartB() {
        return camelContext.getEndpoint("direct:startB");
    }

    @Singleton
    @Named("seda-end-a")
    @javax.enterprise.inject.Produces
    public Endpoint sedaEndA() {
        return camelContext.getEndpoint("seda:endA");
    }

    @Singleton
    @Named("seda-end-b")
    @javax.enterprise.inject.Produces
    public Endpoint sedaEndB() {
        return camelContext.getEndpoint("seda:endB");
    }

    @Singleton
    @Named("my-expression")
    @javax.enterprise.inject.Produces
    public Expression myExpression() {
        return new ExpressionAdapter() {
            @Override
            public Object evaluate(Exchange exchange) {
                return exchange.getMessage().getBody(String.class).toUpperCase();
            }
        };
    }

    @Singleton
    @Named("my-route")
    @javax.enterprise.inject.Produces
    public RoutesBuilder myRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct-start-a")
                        .transform().ref("my-expression")
                        .to("seda-end-a");

                from("ref:direct-start-b")
                        .transform().ref("my-expression")
                        .to("ref:seda-end-b");
            }
        };
    }
}
