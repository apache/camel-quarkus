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

// TODO: investigate why adding resteasy break the platform http component
//@Path("/test")
//@ApplicationScoped
public class PlatformHttpResource {
    /*
    @Inject
    Registry registry;

    @Path("/registry/inspect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public JsonObject inspectRegistry() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        Object engine = registry.lookupByName(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME);
        Object component = registry.lookupByName(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME);

        if (engine != null) {
            builder.add(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME, engine.getClass().getName());

        }if (component != null) {
            builder.add(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME, component.getClass().getName());

        }

        return builder.build();
    }
    */
}
