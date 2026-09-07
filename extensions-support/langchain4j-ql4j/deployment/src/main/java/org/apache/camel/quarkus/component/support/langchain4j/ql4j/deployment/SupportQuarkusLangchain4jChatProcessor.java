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
package org.apache.camel.quarkus.component.support.langchain4j.ql4j.deployment;

import java.util.List;
import java.util.Optional;

import dev.langchain4j.model.chat.ChatModel;
import io.quarkiverse.langchain4j.deployment.RequestChatModelBeanBuildItem;
import io.quarkiverse.langchain4j.deployment.config.LangChain4jBuildConfig;
import io.quarkiverse.langchain4j.deployment.items.ChatModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.runtime.NamedConfigUtil;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import org.jboss.logging.Logger;

/**
 * Build steps for applications combining Quarkus LangChain4j with the Camel LangChain4j chat component.
 */
@BuildSteps(onlyIf = LangChain4jChatPresent.class)
class SupportQuarkusLangchain4jChatProcessor {
    private static final Logger LOG = Logger.getLogger(SupportQuarkusLangchain4jChatProcessor.class);

    /**
     * Quarkus LangChain4j produces a {@code ChatModel} bean only for models that something injects. Requesting the
     * default model here spares applications an otherwise unused injection point, declared purely so that the Camel
     * langchain4j-chat component can autowire the model from the registry.
     *
     * <p>
     * The request is a convenience, so it is made only when it can be satisfied. An application providing its own
     * {@code ChatModel} bean keeps that bean as the sole candidate for autowiring, and an application where the
     * provider of the default model is not determinable is left to build as it did before.
     *
     * <p>
     * Models configured under {@code quarkus.langchain4j.<name>.chat-model.provider} need no such request: Quarkus
     * LangChain4j already produces those from configuration alone.
     */
    @BuildStep
    void defaultChatModelBean(
            List<ChatModelProviderCandidateBuildItem> chatModelProviderCandidates,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinished,
            LangChain4jBuildConfig buildConfig,
            BuildProducer<RequestChatModelBeanBuildItem> requestChatModelBean) {

        if (!beanDiscoveryFinished.beanStream().withBeanType(ChatModel.class).collect().isEmpty()) {
            LOG.debug("Not requesting the default ChatModel bean as the application provides its own");
            return;
        }

        List<String> providers = chatModelProviderCandidates.stream()
                .map(ChatModelProviderCandidateBuildItem::getProvider)
                .toList();
        Optional<String> configuredProvider = buildConfig.defaultConfig().chatModel().provider();
        boolean providerResolvable = configuredProvider.isPresent()
                ? providers.contains(configuredProvider.get())
                : providers.size() == 1;

        if (!providerResolvable) {
            LOG.infof(
                    "Not requesting the default ChatModel bean as its provider could not be determined from the available providers %s. "
                            + "Set quarkus.langchain4j.chat-model.provider, or reference the model explicitly with the langchain4j-chat chatModel endpoint option",
                    providers);
            return;
        }

        requestChatModelBean.produce(new RequestChatModelBeanBuildItem(NamedConfigUtil.DEFAULT_NAME));
    }
}
