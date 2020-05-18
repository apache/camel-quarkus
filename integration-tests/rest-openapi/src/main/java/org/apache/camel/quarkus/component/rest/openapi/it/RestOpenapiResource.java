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
package org.apache.camel.quarkus.component.rest.openapi.it;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.netty.http.NettyHttpComponent;

@Path("/rest-openapi")
public class RestOpenapiResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/fruits/list")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response invokeListFruitsOperation(@QueryParam("port") int port) {
        String response = producerTemplate.requestBodyAndHeader("direct:start", null, "test-port", port, String.class);
        return Response.ok().entity(response).build();
    }

    // TODO: Remove this for Camel 3.4.0. See https://issues.apache.org/jira/browse/CAMEL-15076
    @javax.enterprise.inject.Produces
    @Named("netty-http")
    public NettyHttpComponent createNettyHttpComponent(CamelContext context) {
        NettyHttpComponent nettyHttpComponent = new NettyHttpComponent();
        nettyHttpComponent.setCamelContext(context);
        return nettyHttpComponent;
    }
}
