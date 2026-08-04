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

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Bridge-owned configuration of the Camel MCP server. Serving concerns (endpoint path, transports, authentication)
 * are owned by the quarkiverse quarkus-mcp-server extension and configured via {@code quarkus.mcp.server.*}.
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.camel.mcp-server")
public interface CamelMcpServerConfig {

    /**
     * Whether to expose ai-tool routes as MCP tools through the quarkus-mcp-server extension.
     *
     * @asciidoclet
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Comma-separated list of ai-tool tags to expose as MCP tools. Only tools registered under one of these tags are
     * exposed; the untagged default pool is never exposed. When not set, no tools are exposed.
     *
     * @asciidoclet
     */
    Optional<String> tags();

    /**
     * Per-call tool execution timeout in milliseconds. A call exceeding the timeout returns an error result to the
     * MCP client; the underlying route keeps running until it completes on its own.
     *
     * @asciidoclet
     */
    @WithDefault("20000")
    long toolTimeout();
}
