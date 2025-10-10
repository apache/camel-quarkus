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
package org.apache.camel.quarkus.component.langchain4j.tokenizer.deployment;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import opennlp.tools.util.BaseToolFactory;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class Langchain4jTokenizerProcessor {
    private static final String FEATURE = "camel-langchain4j-tokenizer";
    private static final String TOKENIZER_NOT_FOUND_MESSAGE = "Failed creating tokenizer. Add the required langchain4j LLM model dependencies to your project";
    private static final String[] TOKENIZER_CLASSES = new String[] {
            "dev.langchain4j.model.azure.AzureOpenAiTokenCountEstimator",
            "dev.langchain4j.model.openai.OpenAiTokenCountEstimator",
            "dev.langchain4j.community.model.dashscope.QwenTokenCountEstimator"
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    IndexDependencyBuildItem indexDependencies() {
        return new IndexDependencyBuildItem("org.apache.opennlp", "opennlp-tools");
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResources() {
        return new NativeImageResourceBuildItem("opennlp/opennlp-en-ud-ewt-sentence-1.2-2.5.0.bin");
    }

    @BuildStep
    void registerForReflection(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Set<String> opennlpToolFactories = combinedIndex.getIndex()
                .getAllKnownSubclasses(BaseToolFactory.class)
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toSet());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(opennlpToolFactories.toArray(new String[0]))
                .methods()
                .build());
    }

    @BuildStep(onlyIf = { NativeOrNativeSourcesBuild.class })
    void generateNoOpLangchain4jTokenizer(BuildProducer<GeneratedClassBuildItem> generatedClass) {
        // Generate NoOp impls of langchain4j tokenizers if some or all of the LLM model dependencies are not present
        for (String className : TOKENIZER_CLASSES) {
            try {
                Thread.currentThread().getContextClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                generateLangchain4jTokenizer(generatedClass, className);
            }
        }
    }

    private void generateLangchain4jTokenizer(
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            String className) {

        Class<?>[] initParamTypes = new Class<?>[] { String.class };
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        if (simpleName.startsWith("Qwen")) {
            initParamTypes = new Class<?>[] { String.class, String.class };
        }

        /*
         * Generates a NoOp TokenCountEstimator class to fulfil native compiler requirements.
         *
         * public class OpenAiTokenCountEstimator {
         *     public OpenAiTokenCountEstimator() {
         *         throw new UnsupportedOperationException("Failed creating tokenizer")
         *     }
         * }
         */
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .superClass(Object.class)
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, false))
                .build()) {

            try (MethodCreator initMethod = classCreator.getMethodCreator("<init>", void.class, initParamTypes)) {
                initMethod.setModifiers(Modifier.PUBLIC);
                initMethod.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), initMethod.getThis());
                initMethod.throwException(UnsupportedOperationException.class, TOKENIZER_NOT_FOUND_MESSAGE);
            }
        }
    }
}
