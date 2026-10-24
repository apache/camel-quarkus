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
package org.apache.camel.quarkus.component.mcp.server.it;

import java.util.List;

import io.quarkiverse.mcp.server.ResourceManager;
import io.quarkiverse.mcp.server.ResourceResponse;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Resources registered by the application itself through the quarkus-mcp-server {@link ResourceManager}: one at the
 * same uri as the clashing-resource-route ai-resource route, and one at the uri of the app-config-route ai-resource
 * route, on another MCP server.
 */
@ApplicationScoped
public class QuarkusDefinedResources {

    void registerResources(@Observes StartupEvent event) {
        // looked up, not injected: any ResourceManager injection point makes quarkus-mcp-server advertise
        // resources listChanged, which would hide a missing injection in QuarkusMcpServerEngineFactory
        ResourceManager resourceManager = Arc.container().instance(ResourceManager.class).get();
        resourceManager.newResource("user_resource")
                .setUri("camel:///user/clash")
                .setDescription("Resource registered by the application")
                .setHandler(arguments -> new ResourceResponse(
                        List.of(TextResourceContents.create(arguments.requestUri().value(), "user"))))
                .register();
        resourceManager.newResource("other_config")
                .setUri("camel:///config/app.json")
                .setServerName("other")
                .setDescription("Resource registered by the application on another server")
                .setHandler(arguments -> new ResourceResponse(
                        List.of(TextResourceContents.create(arguments.requestUri().value(), "other"))))
                .register();
    }
}
