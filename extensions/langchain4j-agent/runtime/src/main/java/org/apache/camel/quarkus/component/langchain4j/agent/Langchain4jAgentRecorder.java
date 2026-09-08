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
package org.apache.camel.quarkus.component.langchain4j.agent;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.component.ai.tool.AiToolRegistry;
import org.apache.camel.component.ai.tool.AiToolSpec;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.support.LifecycleStrategySupport;
import org.jboss.logging.Logger;

@Recorder
public class Langchain4jAgentRecorder {

    private static final Logger LOG = Logger.getLogger(Langchain4jAgentRecorder.class);

    /**
     * Creates a customizer that warns when Camel AI tools are registered but the tool bridge shipped with
     * {@code camel-quarkus-ai-tool} is absent, in which case Quarkus LangChain4j AI services receive no Camel tools.
     * Tools are registered as {@code ai-tool:} routes start, so the check runs once the Camel context has started.
     */
    public RuntimeValue<CamelContextCustomizer> createMissingAiToolBridgeWarning() {
        CamelContextCustomizer customizer = camelContext -> camelContext.addLifecycleStrategy(
                LifecycleStrategySupport.adapt(
                        LifecycleStrategySupport.onCamelContextStarted(Langchain4jAgentRecorder::warnIfToolsRegistered)));
        return new RuntimeValue<>(customizer);
    }

    private static void warnIfToolsRegistered(CamelContext camelContext) {
        Set<AiToolSpec> tools = AiToolRegistry.getOrCreate(camelContext).getAllTools();
        if (tools.isEmpty()) {
            return;
        }

        LOG.warnf("Camel AI tools are registered but camel-quarkus-ai-tool is not on the classpath. "
                + "Quarkus LangChain4j AI services will not receive them. "
                + "Add the camel-quarkus-ai-tool dependency to enable the Camel AI tool bridge. "
                + "Affected tools: %s",
                tools.stream().map(AiToolSpec::getName).sorted().collect(Collectors.joining(", ")));
    }
}
