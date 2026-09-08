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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
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

    /**
     * Diagnostics for {@code @CamelAiTools} usage. Runs unconditionally: the pieces the annotation needs are exactly
     * what may be missing, so gating this on their presence would silence the report.
     */
    @BuildStep
    void validateCamelAiToolsUsage(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {

        List<AnnotationInstance> annotations = combinedIndex.getIndex()
                .getAnnotations(CAMEL_AI_TOOLS_DOTNAME)
                .stream()
                .filter(annotation -> annotation.target().kind() == AnnotationTarget.Kind.CLASS)
                .toList();

        if (annotations.isEmpty()) {
            return;
        }

        String affected = annotations.stream()
                .map(annotation -> annotation.target().asClass().name().toString())
                .collect(Collectors.joining(", "));

        if (!new QuarkusLangchain4jPresent().getAsBoolean()) {
            LOG.warnf("@CamelAiTools annotations found but Quarkus LangChain4j is not on the classpath. "
                    + "The annotation only filters tools provided to Quarkus LangChain4j AI services, so it has no "
                    + "effect here. Affected classes: %s", affected);
            return;
        }

        if (!new AiToolPresent().getAsBoolean()) {
            LOG.warnf("@CamelAiTools annotations found but camel-langchain4j-agent is not on the classpath. "
                    + "Add camel-langchain4j-agent dependency to enable the Camel AI tool bridge. "
                    + "Affected classes: %s", affected);
            return;
        }

        for (AnnotationInstance annotation : annotations) {
            if (annotation.value() == null || annotation.value().asString().isBlank()) {
                validationErrors.produce(new ValidationErrorBuildItem(new ConfigurationException(
                        "@CamelAiTools on " + annotation.target().asClass().name() + " has no tag. An empty tag "
                                + "would expose every registered Camel AI tool to the service instead of the "
                                + "intended subset. Give it the tag used by the ai-tool: routes it should see, or "
                                + "remove the annotation to receive all tools deliberately.")));
            }
        }
    }

    @BuildStep(onlyIf = { QuarkusLangchain4jPresent.class, AiToolPresent.class })
    AdditionalBeanBuildItem registerCamelAiToolProvider() {
        LOG.info("Camel AI Tool detected - registering CamelAiToolProvider as CDI bean for ToolProvider auto-discovery");
        return AdditionalBeanBuildItem.unremovableOf(CamelAiToolProvider.class);
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
                // Blank tags are rejected by validateCamelAiToolsUsage
                if (annotation.value() == null || annotation.value().asString().isBlank()) {
                    continue;
                }
                String tagValue = annotation.value().asString();
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
