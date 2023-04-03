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
package org.apache.camel.quarkus.component.dsl.modeline.it;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.event.CamelContextStartedEvent;
import org.apache.camel.spi.DependencyStrategy;
import org.apache.camel.spi.ModelineFactory;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;

@Path("/dsl-modeline")
public class DslModelineResource {

    private List<String> deps;

    private ModelineFactory factory;

    private CamelContext context;

    public void onContextStart(@Observes CamelContextStartedEvent event) {
        context = event.getContext();
        deps = new ArrayList<>();
        context.getRegistry().bind("myDep", (DependencyStrategy) dependency -> deps.add(dependency));
        factory = PluginHelper.getModelineFactory(context);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response parseModeline(String line) throws Exception {
        factory.parseModeline(ResourceHelper.fromString(null, line));
        return Response.ok().build();
    }

    @Path("deps")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getDependencies() {
        return deps;
    }

    @Path("props/{property}")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String getDependencies(@PathParam("property") String property) {
        String uri = String.format("{{%s}}", property);
        return context.getPropertiesComponent().parseUri(uri);
    }

    @DELETE
    public Response clear() {
        deps.clear();
        return Response.ok().build();
    }

}
