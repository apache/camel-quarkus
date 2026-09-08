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
package org.apache.camel.quarkus.component.langchain4j.agent.deployment;

import java.util.function.BooleanSupplier;

/**
 * Whether the LangChain4j tool bridge from {@code camel-quarkus-ai-tool} is missing. Without it, tools registered in
 * the {@code AiToolRegistry} are not exposed to Quarkus LangChain4j AI services.
 */
public class CamelAiToolBridgeAbsent implements BooleanSupplier {
    private static final String CAMEL_AI_TOOL_PROVIDER_CLASS = "org.apache.camel.quarkus.component.ai.tool.CamelAiToolProvider";

    @Override
    public boolean getAsBoolean() {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(CAMEL_AI_TOOL_PROVIDER_CLASS);
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
