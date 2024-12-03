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

import java.util.List;

import io.quarkiverse.langchain4j.deployment.DeclarativeAiServiceBuildItem;
import io.quarkiverse.langchain4j.deployment.items.MethodParameterAllowedAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import static io.quarkus.arc.deployment.UnremovableBeanBuildItem.beanClassNames;

class LangChain4jProcessor {
    private static final String FEATURE = "camel-quarkus-langchain4j";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    MethodParameterAllowedAnnotationsBuildItem camelAnnotatedParametersCouldBeUsedAsTemplateVariable() {
        return new MethodParameterAllowedAnnotationsBuildItem(anno -> anno.name().toString().startsWith("org.apache.camel"));
    };

    @BuildStep
    void markAiServicesAsUnremovable(
            List<DeclarativeAiServiceBuildItem> aiServices, BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        aiServices.stream().forEach(ai -> {
            unremovableBeans.produce(
                    beanClassNames(ai.getServiceClassInfo().name().toString() + "$$QuarkusImpl"));
        });
    };
}
