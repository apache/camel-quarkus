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
package org.apache.camel.quarkus.core;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.Route;
import org.apache.camel.quarkus.core.runtime.CamelRuntime;

@Path("/")
@ApplicationScoped
public class CamelServlet {
    @Inject
    CamelRuntime runtime;

    @Path("/routes")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public List<String> getRoutes() {
        return runtime.getContext().getRoutes().stream().map(Route::getId).collect(Collectors.toList());
    }

    @Path("/property/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getProperty(@PathParam("name") String name) throws Exception {
        String prefix = runtime.getContext().getPropertyPrefixToken();
        String suffix = runtime.getContext().getPropertySuffixToken();

        return runtime.getContext().resolvePropertyPlaceholders(prefix + name + suffix);
    }
}
