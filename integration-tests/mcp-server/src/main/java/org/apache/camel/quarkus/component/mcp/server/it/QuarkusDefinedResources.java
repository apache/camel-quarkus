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

import io.quarkiverse.mcp.server.RequestUri;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.ResourceManager;
import io.quarkiverse.mcp.server.ResourceResponse;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Resources defined by the application itself.
 * <p>
 * With {@link Resource}, registered at build time before any route starts: one per outcome an ai-resource route can
 * meet, named after it. Each returns its own content, so a read tells which resource served it.
 * <p>
 * With {@link ResourceManager}, registered on startup: one at the uri of the clashing-resource-route ai-resource
 * route, and one at the uri of the app-config-route ai-resource route, on another MCP server.
 */
@ApplicationScoped
public class QuarkusDefinedResources {

    public static final String NO_CLASH_CONTENT = "read from @Resource annotated_no_clash";
    public static final String URI_CLASH_CONTENT = "read from @Resource annotated_uri_clash";
    public static final String NAME_CLASH_CONTENT = "read from @Resource annotated_name_clash";

    /** Clashes with nothing: exposed next to the Camel resources. */
    @Resource(name = "annotated_no_clash", uri = "camel:///annotated/no-clash", description = "Resource declared with @Resource, clashing with nothing", mimeType = "text/plain")
    ResourceResponse annotatedNoClash(RequestUri uri) {
        return new ResourceResponse(List.of(TextResourceContents.create(uri.value(), NO_CLASH_CONTENT)));
    }

    /** Owns the uri of the camel_uri_clash route, whose name is free: that route is refused on the uri. */
    @Resource(name = "annotated_uri_clash", uri = "camel:///annotated/uri-clash", description = "Uri also asked for by an ai-resource route")
    ResourceResponse annotatedUriClash(RequestUri uri) {
        return new ResourceResponse(List.of(TextResourceContents.create(uri.value(), URI_CLASH_CONTENT)));
    }

    /** Owns the name of the annotated_name_clash route, whose uri is free: that route is refused on the name. */
    @Resource(name = "annotated_name_clash", uri = "camel:///annotated/name-clash", description = "Name also asked for by an ai-resource route")
    ResourceResponse annotatedNameClash(RequestUri uri) {
        return new ResourceResponse(List.of(TextResourceContents.create(uri.value(), NAME_CLASH_CONTENT)));
    }

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
