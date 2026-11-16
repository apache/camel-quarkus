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
package org.apache.camel.quarkus.component.mcp.server;

import io.quarkiverse.mcp.server.PromptManager;
import io.quarkiverse.mcp.server.ResourceManager;
import io.quarkiverse.mcp.server.ResourceTemplateManager;
import io.quarkiverse.mcp.server.ToolManager;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Creates the {@link QuarkusMcpServerEngine}. The managers are injected rather than looked up programmatically:
 * quarkus-mcp-server advertises the {@code listChanged} capability of tools, resources, resource templates and prompts
 * when their manager has an injection point, and the Camel features change with the route lifecycle.
 */
@Singleton
public class QuarkusMcpServerEngineFactory {

    @Inject
    ToolManager toolManager;

    @Inject
    ResourceManager resourceManager;

    @Inject
    ResourceTemplateManager resourceTemplateManager;

    @Inject
    PromptManager promptManager;

    // Security is optional for applications exposing unauthenticated MCP endpoints.
    @Inject
    Instance<CurrentIdentityAssociation> currentIdentityAssociation;

    QuarkusMcpServerEngine createEngine() {
        CurrentIdentityAssociation identityAssociation = currentIdentityAssociation.isResolvable()
                ? currentIdentityAssociation.get()
                : null;
        return new QuarkusMcpServerEngine(
                toolManager, resourceManager, resourceTemplateManager, promptManager, identityAssociation);
    }
}
