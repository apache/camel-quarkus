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

import java.util.List;

import dev.langchain4j.model.chat.ChatModel;
import io.quarkiverse.langchain4j.deployment.items.SelectedChatModelProviderBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.component.support.langchain4j.Langchain4jRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanQualifierResolverBuildItem;

class SupportLangchain4jProcessor {
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void chatModelBeanQualifiers(
            List<SelectedChatModelProviderBuildItem> chatModels,
            BuildProducer<CamelBeanQualifierResolverBuildItem> beanQualifierResolver,
            Langchain4jRecorder recorder) {

        // Enable ChatLanguageModel instances to be resolved by name from the Camel registry
        for (SelectedChatModelProviderBuildItem chatModel : chatModels) {
            beanQualifierResolver.produce(
                    new CamelBeanQualifierResolverBuildItem(ChatModel.class,
                            chatModel.getConfigName(),
                            recorder.chatModelBeanQualifierResolver(chatModel.getConfigName())));
        }
    }

    // TODO: Remove this: https://github.com/apache/camel-quarkus/issues/7440
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
                "dev.langchain4j.model.ollama.ToolCall")
                .methods(true)
                .build();
    }
}
