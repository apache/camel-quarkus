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
package org.apache.camel.quarkus.component.bean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.bean.model.Employee;

@Path("/bean")
@ApplicationScoped
public class BeanResource {
    @Inject
    ProducerTemplate template;

    @Inject
    Counter counter;

    @Inject
    EagerAppScopedRouteBuilder routeBuilder;

    @Inject
    EndpointInjectBean endpointInjectBean;

    @Inject
    ConsumerTemplate endpointInjectConsumer;

    @Path("/route/{route}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String route(String statement, @PathParam("route") String route) {
        return template.requestBody("direct:" + route, statement, String.class);
    }

    @Path("/route/{route}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String route(@PathParam("route") String route) {
        return template.requestBody("direct:" + route, null, String.class);
    }

    @Path("/beanMethodInHeader")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String beanMethodInHeader(String statement) {
        return template.requestBodyAndHeader(
                "direct:named",
                statement,
                Exchange.BEAN_METHOD_NAME,
                "hi",
                String.class);
    }

    @Path("/counter")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int counter() {
        return counter.getValue();
    }

    @Path("/route-builder-instance-counter")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int routeBuilderInstanceCounter() {
        return EagerAppScopedRouteBuilder.INSTANCE_COUNTER.get();
    }

    @Path("/route-builder-configure-counter")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int routeBuilderConfigureCounter() {
        return EagerAppScopedRouteBuilder.CONFIGURE_COUNTER.get();
    }

    @Path("/route-builder-injected-count")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int routeBuilderInjectedCount() {
        return routeBuilder.getCounter().getValue();
    }

    @Path("/camel-configure-counter")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int lazyConfigureCounter() {
        return BeanRoutes.CONFIGURE_COUNTER.get();
    }

    @Path("/endpointInject")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String endpointInject(String payload) {
        endpointInjectBean.forward(payload);
        return endpointInjectConsumer.receiveBody("direct:endpointInject", 10000, String.class);
    }

    @Path("/employee/{route}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String methodWithExchangeArg(Employee employee, @PathParam("route") String route) {
        return template.requestBody("direct:" + route, employee, String.class);
    }

    @Path("/parameterBindingAnnotations/{greeting}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String parameterBindingAnnotations(String statement, @PathParam("greeting") String greeting) {
        return template.requestBodyAndHeader(
                "direct:parameterBindingAnnotations",
                statement,
                "parameterBinding.greeting",
                greeting,
                String.class);
    }

}
