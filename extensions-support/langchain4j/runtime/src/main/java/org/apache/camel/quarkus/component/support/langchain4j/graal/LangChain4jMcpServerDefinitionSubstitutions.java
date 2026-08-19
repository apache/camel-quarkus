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
package org.apache.camel.quarkus.component.support.langchain4j.graal;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import org.apache.camel.CamelContext;
import org.apache.camel.support.OAuthHelper;

/**
 * The original method builds the deprecated SSE transport with
 * dev.langchain4j.mcp.client.transport.http.HttpMcpTransport,
 * which was removed in LangChain4j 1.19.0. The unresolvable type makes native image analysis of the method fail, so the
 * SSE branch is replaced here by an error explaining that the transport is gone. The other transports are unchanged.
 *
 * TODO: Remove this class when Camel drops its usage of HttpMcpTransport -
 * https://github.com/apache/camel-quarkus/issues/9025
 */
@TargetClass(className = "org.apache.camel.component.langchain4j.agent.LangChain4jMcpServerDefinition", onlyWith = {
        LangChain4jMcpServerDefinitionSubstitutions.SseTransportUnavailable.class })
final class LangChain4jMcpServerDefinitionSubstitutions {
    @Alias
    private String transportType;
    @Alias
    private List<String> command;
    @Alias
    private Map<String, String> environment;
    @Alias
    private String url;
    @Alias
    private Duration timeout;
    @Alias
    private boolean logRequests;
    @Alias
    private boolean logResponses;
    @Alias
    private String oauthProfile;

    @Substitute
    private McpTransport buildTransport(CamelContext camelContext) throws Exception {
        // Resolve OAuth token for HTTP-based transports
        Map<String, String> authHeaders = null;
        if (oauthProfile != null && !oauthProfile.isBlank() && camelContext != null) {
            String token = OAuthHelper.resolveOAuthToken(camelContext, oauthProfile);
            authHeaders = Map.of("Authorization", "Bearer " + token);
        }

        if ("http".equalsIgnoreCase(transportType) || "streamableHttp".equalsIgnoreCase(transportType)) {
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("URL is required for HTTP MCP transport");
            }
            StreamableHttpMcpTransport.Builder builder = new StreamableHttpMcpTransport.Builder()
                    .url(url)
                    .logRequests(logRequests)
                    .logResponses(logResponses)
                    .timeout(timeout);
            if (authHeaders != null) {
                builder.customHeaders(authHeaders);
            }
            return builder.build();
        } else if ("stdio".equalsIgnoreCase(transportType)) {
            if (command == null || command.isEmpty()) {
                throw new IllegalArgumentException("Command is required for stdio MCP transport");
            }
            StdioMcpTransport.Builder builder = new StdioMcpTransport.Builder()
                    .command(command)
                    .logEvents(logRequests || logResponses);
            if (environment != null) {
                builder.environment(environment);
            }
            return builder.build();
        } else if ("sse".equalsIgnoreCase(transportType)) {
            throw new IllegalArgumentException(
                    "The sse MCP transport is unavailable since HttpMcpTransport was removed in LangChain4j 1.19.0."
                            + " Use the streamableHttp transport instead.");
        } else {
            throw new IllegalArgumentException(
                    "Unsupported MCP transport type: " + transportType
                            + ". Supported values: stdio, http, streamableHttp");
        }
    }

    static final class SseTransportUnavailable implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            return isPresent(classLoader, "org.apache.camel.component.langchain4j.agent.LangChain4jMcpServerDefinition")
                    && !isPresent(classLoader, "dev.langchain4j.mcp.client.transport.http.HttpMcpTransport");
        }

        private boolean isPresent(ClassLoader classLoader, String className) {
            try {
                classLoader.loadClass(className);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
