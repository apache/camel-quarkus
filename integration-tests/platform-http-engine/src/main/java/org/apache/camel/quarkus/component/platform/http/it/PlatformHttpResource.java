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
package org.apache.camel.quarkus.component.platform.http.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.quarkus.component.platform.http.runtime.QuarkusPlatformHttpEngine;
import org.apache.camel.spi.Registry;

@Path("/test")
@ApplicationScoped
public class PlatformHttpResource {
    @Inject
    Registry registry;

    @Path("/registry/inspect")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspectRegistry() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        Object engine = registry.lookupByName(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME);
        Object component = registry.lookupByName(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME);

        if (engine != null) {
            builder.add(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME, engine.getClass().getName());

            if (engine instanceof QuarkusPlatformHttpEngine) {
                builder.add("handlers-size", ((QuarkusPlatformHttpEngine) engine).getHandlers().size());
            }
        }

        if (component != null) {
            builder.add(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME, component.getClass().getName());
        }

        return builder.build();
    }
}
