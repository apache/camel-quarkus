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
package org.apache.camel.quarkus.component.langchain.chat.deployment;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class LangChain4jChatProcessor {
    private static final String FEATURE = "camel-langchain4j-chat";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageProxyDefinitionBuildItem nativeImageProxyConfig() {
        return new NativeImageProxyDefinitionBuildItem("dev.langchain4j.model.ollama.OllamaApi");
    }

    @BuildStep
    ReflectiveClassBuildItem reflectiveClass() {
        return ReflectiveClassBuildItem.builder(PropertyNamingStrategies.SnakeCaseStrategy.class).constructors().build();
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return ReflectiveClassBuildItem.builder(
                "dev.langchain4j.model.ollama.FormatSerializer",
                "dev.langchain4j.model.ollama.Function",
                "dev.langchain4j.model.ollama.FunctionCall",
                "dev.langchain4j.model.ollama.Message",
                "dev.langchain4j.model.ollama.OllamaChatRequest",
                "dev.langchain4j.model.ollama.OllamaChatResponse",
                "dev.langchain4j.model.ollama.Options",
                "dev.langchain4j.model.ollama.Parameters",
                "dev.langchain4j.model.ollama.Role",
                "dev.langchain4j.model.ollama.Tool",
                "dev.langchain4j.model.ollama.ToolCall",
                "dev.langchain4j.model.ollama.ChatRequest",
                "dev.langchain4j.model.ollama.ChatResponse")
                .methods(true)
                .build();
    }
}
