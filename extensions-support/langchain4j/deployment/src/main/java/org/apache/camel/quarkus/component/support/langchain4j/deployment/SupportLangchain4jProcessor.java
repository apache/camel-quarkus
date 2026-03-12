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

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.djl.util.NativeResource;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.JsonExtractorOutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.model.embedding.onnx.AbstractInProcessEmbeddingModel;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.common.os.OS;
import opennlp.tools.sentdetect.SentenceDetectorFactory;
import org.apache.camel.util.ObjectHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

@BuildSteps(onlyIf = NativeOrNativeSourcesBuild.class)
class SupportLangchain4jProcessor {
    private static final Class<?>[] AI_SERVICE_ANNOTATION_CLASSES = {
            MemoryId.class,
            SystemMessage.class,
            UserMessage.class,
            V.class
    };

    @BuildStep
    void indexDependencies(CurateOutcomeBuildItem curateOutcome, BuildProducer<IndexDependencyBuildItem> indexedDependencies) {
        ApplicationModel applicationModel = curateOutcome.getApplicationModel();
        for (ResolvedDependency dependency : applicationModel.getDependencies()) {
            if (dependency.getGroupId().equals("dev.langchain4j")) {
                indexedDependencies.produce(new IndexDependencyBuildItem(dependency.getGroupId(), dependency.getArtifactId()));
            }
        }
    }

    @BuildStep
    ServiceProviderBuildItem registerServiceProviders() {
        return ServiceProviderBuildItem.allProvidersFromClassPath("dev.langchain4j.http.client.HttpClientBuilderFactory");
    }

    @BuildStep
    void registerLangChain4jJacksonTypesForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        IndexView index = combinedIndex.getIndex();

        // Discover all LangChain4j Jackson model types
        Set<String> langChain4jModelClasses = langChain4jTypesStream(index.getKnownClasses())
                .filter(classInfo -> classInfo.annotations().stream()
                        .anyMatch(annotationInstance -> annotationInstance.name().toString()
                                .startsWith("com.fasterxml.jackson.annotation")))
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toSet());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(langChain4jModelClasses.toArray(new String[0]))
                .fields()
                .methods()
                .build());

        // Discover all LangChain4j Jackson serializer / deserializer types
        Set<String> jacksonSupportClasses = langChain4jTypesStream(index.getAllKnownSubclasses(JsonSerializer.class))
                .map(classInfo -> classInfo.name().toString())
                .collect(Collectors.toSet());

        langChain4jTypesStream(index.getAllKnownSubclasses(JsonDeserializer.class))
                .map(classInfo -> classInfo.name().toString())
                .forEach(jacksonSupportClasses::add);

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(jacksonSupportClasses.toArray(new String[0])).build());

        // Misc Jackson support
        ReflectiveClassBuildItem.builder(PropertyNamingStrategies.SnakeCaseStrategy.class).build();
    }

    @BuildStep
    void registerLangChain4jAiServiceTypesForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageProxyDefinitionBuildItem> nativeImageProxy) {

        IndexView index = combinedIndex.getIndex();
        Set<String> aiServiceInterfaces = new HashSet<>();
        Set<String> aiServiceTypes = new HashSet<>();

        for (Class<?> aiServiceClass : AI_SERVICE_ANNOTATION_CLASSES) {
            for (AnnotationInstance annotationInstance : index.getAnnotations(aiServiceClass)) {
                AnnotationTarget annotationTarget = annotationInstance.target();

                if (annotationTarget.kind().equals(AnnotationTarget.Kind.CLASS)) {
                    aiServiceInterfaces.add(annotationTarget.asClass().name().toString());
                } else if (annotationTarget.kind().equals(AnnotationTarget.Kind.METHOD)) {
                    MethodInfo method = annotationTarget.asMethod();
                    aiServiceInterfaces.add(method.declaringClass().name().toString());
                    if (!method.returnType().kind().equals(Type.Kind.VOID)) {
                        aiServiceTypes.add(method.returnType().name().toString());
                    }
                } else if (annotationTarget.kind().equals(AnnotationTarget.Kind.METHOD_PARAMETER)) {
                    MethodParameterInfo methodParameter = annotationTarget.asMethodParameter();
                    aiServiceTypes.add(methodParameter.type().name().toString());

                    MethodInfo method = methodParameter.method();
                    aiServiceInterfaces.add(method.declaringClass().name().toString());
                    if (!method.returnType().kind().equals(Type.Kind.VOID)) {
                        aiServiceTypes.add(method.returnType().name().toString());
                    }
                }
            }
        }

        // Any types participating in JsonExtractorOutputGuardrail operations require reflection
        index.getAllKnownSubclasses(JsonExtractorOutputGuardrail.class)
                .stream()
                .filter(classInfo -> classInfo.superClassType() != null)
                .filter(classInfo -> classInfo.superClassType().kind().equals(Type.Kind.PARAMETERIZED_TYPE))
                .map(ClassInfo::superClassType)
                .map(Type::asParameterizedType)
                .flatMap(type -> type.arguments().stream())
                .findFirst()
                .ifPresent(typeParameter -> {
                    aiServiceTypes.add(typeParameter.name().toString());
                });

        // AI service interfaces must be registered as native image proxies
        aiServiceInterfaces
                .stream()
                .map(NativeImageProxyDefinitionBuildItem::new)
                .forEach(nativeImageProxy::produce);

        // Register any types related to the AI service for reflection
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(aiServiceTypes.toArray(new String[0]))
                .fields()
                .methods()
                .build());

        // Guardrails are instantiated dynamically
        Set<String> guardrailTypes = index.getAllKnownImplementations(InputGuardrail.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .collect(Collectors.toSet());

        index.getAllKnownImplementations(OutputGuardrail.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .forEach(guardrailTypes::add);

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(guardrailTypes.toArray(new String[0])).build());
    }

    @BuildStep
    void registerCustomToolsForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        Set<String> customToolClasses = combinedIndex.getIndex()
                .getAnnotations(Tool.class)
                .stream()
                .map(AnnotationInstance::target)
                .map(AnnotationTarget::asMethod)
                .map(MethodInfo::declaringClass)
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toSet());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(customToolClasses.toArray(new String[0]))
                .methods()
                .build());
    }

    @BuildStep
    void registerLangChain4jNlpTypesForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(SentenceDetectorFactory.class).build());
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClasses() {
        return new RuntimeInitializedClassBuildItem("dev.langchain4j.internal.RetryUtils");
    }

    @BuildStep
    NativeImageResourcePatternsBuildItem nativeImageResources() {
        return NativeImageResourcePatternsBuildItem.builder()
                .includeGlob("opennlp/*.bin")
                .build();
    }

    static Stream<ClassInfo> langChain4jTypesStream(Collection<ClassInfo> classes) {
        return classes.stream()
                .filter(classInfo -> classInfo.name().toString().startsWith("dev.langchain4j"));
    }

    @BuildStep
    void indexDjlDependencies(
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

    @BuildStep
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
            throw new RuntimeException("%s does not support platform %s-%s".formatted("SupportLangchain4j", os, osArch));
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

    @BuildStep
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

    @BuildStep
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
