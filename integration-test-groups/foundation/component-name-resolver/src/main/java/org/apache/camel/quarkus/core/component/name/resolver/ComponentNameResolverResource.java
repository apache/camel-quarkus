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
package org.apache.camel.quarkus.core.component.name.resolver;

import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.ComponentNameResolver;
import org.apache.camel.support.PluginHelper;

@Path("/component-name-resolver")
public class ComponentNameResolverResource {

    @Inject
    CamelContext context;

    @Path("/class")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String configuredComponentNameResolverClass() {
        ComponentNameResolver resolver = PluginHelper.getComponentNameResolver(context);
        return resolver.getClass().getName();
    }

    @Path("/resolve")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String fastResolveComponentNames() {
        ComponentNameResolver resolver = PluginHelper.getComponentNameResolver(context);
        return resolver.resolveNames(context)
                .stream()
                .collect(Collectors.joining(","));
    }
}
