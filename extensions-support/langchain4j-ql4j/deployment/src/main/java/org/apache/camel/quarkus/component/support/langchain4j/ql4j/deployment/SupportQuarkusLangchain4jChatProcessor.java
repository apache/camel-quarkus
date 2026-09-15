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

import io.quarkiverse.langchain4j.deployment.RequestChatModelBeanBuildItem;
import io.quarkiverse.langchain4j.runtime.NamedConfigUtil;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;

/**
 * Build steps for applications combining Quarkus LangChain4j with the Camel LangChain4j chat component.
 */
@BuildSteps(onlyIf = LangChain4jChatPresent.class)
class SupportQuarkusLangchain4jChatProcessor {

    /**
     * Quarkus LangChain4j produces a {@code ChatModel} bean only for models that something injects. Requesting the
     * default model here spares applications an otherwise unused injection point, declared purely so that the Camel
     * langchain4j-chat component can autowire the model from the registry.
     *
     * <p>
     * Models configured under {@code quarkus.langchain4j.<name>.chat-model.provider} need no such request: Quarkus
     * LangChain4j already produces those from configuration alone.
     */
    @BuildStep
    RequestChatModelBeanBuildItem defaultChatModelBean() {
        return new RequestChatModelBeanBuildItem(NamedConfigUtil.DEFAULT_NAME);
    }
}
