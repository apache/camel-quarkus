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

import io.quarkiverse.mcp.server.ToolManager;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.mcp.server.McpServerBridge;
import org.apache.camel.component.mcp.server.McpServerConfiguration;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.spi.CamelContextCustomizer;

@Recorder
public class CamelMcpServerRecorder {

    public RuntimeValue<CamelContextCustomizer> createContextCustomizer(String tags, long toolTimeout) {
        return new RuntimeValue<>(new CamelContextCustomizer() {
            @Override
            public void configure(CamelContext camelContext) {
                ToolManager toolManager = Arc.container().instance(ToolManager.class).get();
                QuarkusMcpServerEngine engine = new QuarkusMcpServerEngine(toolManager);

                McpServerConfiguration configuration = new McpServerConfiguration();
                configuration.setTags(tags);
                configuration.setToolTimeout(toolTimeout);
                McpServerBridge bridge = new McpServerBridge(configuration);
                try {
                    // the bridge resolves the engine registry-first
                    camelContext.getRegistry().bind("quarkusCamelMcpServerEngine", McpServerEngine.class, engine);
                    camelContext.addService(bridge);
                } catch (Exception e) {
                    throw RuntimeCamelException.wrapRuntimeCamelException(e);
                }
            }
        });
    }
}
