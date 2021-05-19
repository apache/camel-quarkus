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
package org.apache.camel.quarkus.component.bean.eip;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

@Path("/bean/eip")
@ApplicationScoped
public class EipResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/route/{route}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String route(String body, @PathParam("route") String route) {
        return producerTemplate.requestBody("direct:" + route, body, String.class);
    }

    @Path("/result/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String mock(@PathParam("name") String name, @PathParam("count") int count, @PathParam("timeout") int timeout) {
        List<String> results = context.getRegistry().lookupByNameAndType(name, List.class);
        return results.stream().collect(Collectors.joining(","));
    }
}
