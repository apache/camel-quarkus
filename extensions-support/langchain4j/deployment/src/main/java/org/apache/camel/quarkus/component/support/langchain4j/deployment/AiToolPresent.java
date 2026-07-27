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
package org.apache.camel.quarkus.component.support.langchain4j.deployment;

import java.util.function.BooleanSupplier;

public class AiToolPresent implements BooleanSupplier {
    private static final String AI_TOOL_REGISTRY_CLASS = "org.apache.camel.component.ai.tool.AiToolRegistry";
    private static final String AI_TOOL_SPEC_CONVERTER_CLASS = "org.apache.camel.component.langchain4j.agent.AiToolSpecToLangChain4j";

    @Override
    public boolean getAsBoolean() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            cl.loadClass(AI_TOOL_REGISTRY_CLASS);
            cl.loadClass(AI_TOOL_SPEC_CONVERTER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
