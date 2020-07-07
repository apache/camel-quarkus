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
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;

@Path("/bean")
@ApplicationScoped
public class CamelResource {
    @Inject
    ProducerTemplate template;

    @Inject
    Counter counter;

    @Inject
    EagerAppScopedRouteBuilder routeBuilder;

    @Path("/process-order")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String processOrder(String statement) {
        return template.requestBody("direct:process-order", statement, String.class);
    }

    @Path("/named")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String named(String statement) {
        return template.requestBody("direct:named", statement, String.class);
    }

    @Path("/method")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String method(String statement) {
        return template.requestBody("direct:method", statement, String.class);
    }

    @Path("/handler")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String handler(String statement) {
        return template.requestBody("direct:handler", statement, String.class);
    }

    @Path("/increment")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String increment() {
        return template.requestBody("direct:counter", null, String.class);
    }

    @Path("/counter")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int counter() {
        return counter.getValue();
    }

    @Path("/config-property")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String configProperty() {
        return template.requestBody("direct:config-property", null, String.class);
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

    @Path("/lazy")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String lazy() {
        return template.requestBody("direct:lazy", null, String.class);
    }

    @Path("/camel-configure-counter")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int lazyConfigureCounter() {
        return CamelRoute.CONFIGURE_COUNTER.get();
    }

    @Path("/with-producer")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String withProducer() {
        return template.requestBody("direct:with-producer", null, String.class);
    }

}
