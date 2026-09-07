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
package org.apache.camel.quarkus.component.ai.tool.deployment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.camel.quarkus.component.ai.tool.AiToolRecorder;
import org.apache.camel.quarkus.component.ai.tool.AiToolSpecConverterImpl;
import org.apache.camel.quarkus.component.ai.tool.CamelAiToolProvider;
import org.apache.camel.quarkus.component.ai.tool.CamelAiToolsInterceptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

class AiToolProcessor {

    private static final String FEATURE = "camel-ai-tool";
    private static final DotName CAMEL_AI_TOOLS_DOTNAME = DotName
            .createSimple("org.apache.camel.quarkus.component.ai.tool.CamelAiTools");

    private static final Logger LOG = Logger.getLogger(AiToolProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = QuarkusLangchain4jPresent.class)
    void registerCamelAiToolProvider(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        if (!new AiToolPresent().getAsBoolean()) {
            Collection<AnnotationInstance> aiToolsAnnotations = combinedIndex.getIndex()
                    .getAnnotations(CAMEL_AI_TOOLS_DOTNAME);
            if (!aiToolsAnnotations.isEmpty()) {
                LOG.warnf("@CamelAiTools annotations found but camel-langchain4j-agent is not on the classpath. "
                        + "Add camel-langchain4j-agent dependency to enable the Camel AI tool bridge. "
                        + "Affected classes: %s",
                        aiToolsAnnotations.stream()
                                .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                                .map(a -> a.target().asClass().name().toString())
                                .collect(Collectors.joining(", ")));
            }
            return;
        }

        LOG.info("Camel AI Tool detected - registering CamelAiToolProvider as CDI bean for ToolProvider auto-discovery");
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(CamelAiToolProvider.class));
    }

    @BuildStep(onlyIf = { QuarkusLangchain4jPresent.class, AiToolPresent.class })
    AdditionalBeanBuildItem registerAiToolSpecConverter() {
        return AdditionalBeanBuildItem.unremovableOf(AiToolSpecConverterImpl.class);
    }

    @BuildStep(onlyIf = { QuarkusLangchain4jPresent.class, AiToolPresent.class })
    @Record(ExecutionTime.STATIC_INIT)
    void configureCamelAiToolTags(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            AiToolRecorder recorder) {

        IndexView index = combinedIndex.getIndex();
        Map<String, String> tagMap = new HashMap<>();
        for (AnnotationInstance annotation : index.getAnnotations(CAMEL_AI_TOOLS_DOTNAME)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                String className = annotation.target().asClass().name().toString();
                if (annotation.value() == null) {
                    LOG.warnf("@CamelAiTools on %s has no value — skipping", className);
                    continue;
                }
                String tagValue = annotation.value().asString();
                if (tagValue.isBlank()) {
                    LOG.warnf("@CamelAiTools on %s has blank value — skipping", className);
                    continue;
                }
                tagMap.put(className, tagValue);
                LOG.infof("Discovered @CamelAiTools(\"%s\") on %s", tagValue, className);
            }
        }

        if (tagMap.isEmpty()) {
            return;
        }

        recorder.setCamelAiToolTagMap(tagMap);

        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(CamelAiToolsInterceptor.class)
                .setUnremovable()
                .build());
    }
}
