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
package org.apache.camel.quarkus.component.langchain.embeddings.deployment;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.stream.Stream;

import ai.djl.util.NativeResource;
import dev.langchain4j.model.embedding.onnx.AbstractInProcessEmbeddingModel;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.common.os.OS;
import org.apache.camel.util.ObjectHelper;
import org.jboss.jandex.IndexView;

class Langchain4jEmbeddingsProcessor {
    private static final String FEATURE = "camel-langchain4j-embeddings";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void indexDependencies(
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<IndexDependencyBuildItem> indexDependency) {

        ApplicationModel applicationModel = curateOutcome.getApplicationModel();
        for (ResolvedDependency dependency : applicationModel.getDependencies()) {
            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();

            // Index ai.djl dependencies
            if (groupId.startsWith("ai.djl")) {
                indexDependency.produce(new IndexDependencyBuildItem(groupId, artifactId));
            }
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void nativeImageResources(
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<NativeImageResourcePatternsBuildItem> nativeImageResourcePattern,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResource) {

        // The supported platforms for native binaries within ai.djl.huggingface:tokenizers & com.microsoft.onnxruntime:onnxruntime
        String os = switch (OS.current()) {
        case LINUX -> "linux";
        case WINDOWS -> "win";
        case MAC -> "osx";
        default -> null;
        };

        // Search for native binaries for the target platform and add them to the native image
        String osArch = System.getProperty("os.arch");
        if (ObjectHelper.isNotEmpty(os)) {
            Stream.of(AI_LIBRARY.values()).forEach(aiLibrary -> {
                try {
                    String binaryPath = aiLibrary.getBinaryPath(os);
                    Enumeration<URL> resources = Thread.currentThread()
                            .getContextClassLoader()
                            .getResources(binaryPath);
                    if (resources.hasMoreElements()) {
                        nativeImageResourcePattern
                                .produce(NativeImageResourcePatternsBuildItem.builder()
                                        .includeGlobs(binaryPath + "/**").build());
                    } else {
                        throw new RuntimeException(
                                "Failed to find required native binaries on classpath at %s".formatted(binaryPath));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            throw new RuntimeException("%s does not support platform %s-%s".formatted(FEATURE, os, osArch));
        }

        nativeImageResource.produce(new NativeImageResourceBuildItem("native/lib/tokenizers.properties"));

        for (ResolvedDependency dependency : curateOutcome.getApplicationModel().getDependencies()) {
            String artifactId = dependency.getArtifactId();

            // Native image resources for embeddings tokenizer metadata
            String prefix = "langchain4j-embeddings-";
            if (artifactId.startsWith(prefix)) {
                String embeddingLibType = artifactId.substring(prefix.length());
                nativeImageResource.produce(new NativeImageResourceBuildItem(embeddingLibType + ".onnx"));
                nativeImageResource.produce(new NativeImageResourceBuildItem(embeddingLibType + "-tokenizer.json"));
            }
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void jniRuntimeSupport(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<JniRuntimeAccessBuildItem> jniRuntimeAccess) {

        String[] jniReflectiveClasses = new String[] {
                "ai.onnxruntime.TensorInfo",
                "ai.onnxruntime.SequenceInfo",
                "ai.onnxruntime.MapInfo",
                "ai.onnxruntime.OrtException",
                "ai.onnxruntime.OnnxSparseTensor",
                "ai.djl.huggingface.tokenizers.jni.CharSpan",
                "[Lai.djl.huggingface.tokenizers.jni.CharSpan;",
                "[Lai.onnxruntime.OnnxValue;",
                "[Ljava.lang.String;",
                "ai.onnxruntime.OnnxTensor",
                "ai.onnxruntime.OnnxValue",
                "ai.onnxruntime.TensorInfo",
                "ai.onnxruntime.OnnxTensor",
                "java.lang.Boolean",
                "java.lang.String"
        };

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(jniReflectiveClasses)
                        .fields()
                        .methods()
                        .build());

        jniRuntimeAccess.produce(
                new JniRuntimeAccessBuildItem(true, true, true, jniReflectiveClasses));

        reflectiveClass.produce(ReflectiveClassBuildItem.builder("opennlp.tools.sentdetect.SentenceDetectorFactory").build());
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void runtimeInitializedClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {

        IndexView index = combinedIndex.getIndex();

        // Due to JNI usage the following types must be initialized at runtime
        index.getAllKnownSubclasses(AbstractInProcessEmbeddingModel.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);

        index.getAllKnownSubclasses(NativeResource.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);

        Stream.of("ai.djl.huggingface.tokenizers.jni.TokenizersLibrary",
                "ai.djl.huggingface.tokenizers.jni.LibUtils",
                "ai.djl.util.Platform",
                "ai.onnxruntime.OrtEnvironment",
                "ai.onnxruntime.OnnxRuntime",
                "ai.onnxruntime.OnnxTensorLike",
                "ai.onnxruntime.OrtSession$SessionOptions")
                .filter(QuarkusClassLoader::isClassPresentAtRuntime)
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

    enum AI_LIBRARY {
        AI_DJL_HUGGINGFACE_TOKENIZERS("ai/onnxruntime/native/%s-%s", "x64"),
        COM_MICROSOFT_ONNXRUNTIME("native/lib/%s-%s/cpu", "x86_64");

        private final String binaryPathTemplate;
        private final String x86Arch;

        AI_LIBRARY(String binaryPathTemplate, String x86Arch) {
            this.binaryPathTemplate = binaryPathTemplate;
            this.x86Arch = x86Arch;
        }

        public String getBinaryPath(String os) {
            return binaryPathTemplate.formatted(os, getBinaryArch());
        }

        public String getBinaryArch() {
            String arch = System.getProperty("os.arch");
            if (arch.equals("amd64")) {
                return this.x86Arch;
            }
            return arch;
        }
    }
}
