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
package org.apache.camel.quarkus.component.openai.deployment;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.openai.core.JsonField;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.jackson.deployment.IgnoreJsonDeserializeClassBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.paths.PathFilter;
import org.apache.camel.util.StringHelper;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

class OpenaiProcessor {
    private static final String FEATURE = "camel-openai";
    private static final String OPENAI_MODELS_PACKAGE = "com.openai.models.";
    // The model packages backing the operations of the Camel OpenAI component, plus the packages they depend on
    private static final Set<String> OPENAI_MODELS_SUB_PACKAGES = Set.of(
            "audio",
            "chat",
            "completions",
            "embeddings",
            "responses");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.modelcontextprotocol.sdk", "mcp-core"));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void indexOpenAIClasses(
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses) {
        // openai-java-core is not indexed as a whole. Around 80% of its model classes (beta, admin, realtime, evals etc.)
        // are not used by the Camel OpenAI component. If they were indexed, quarkus-jackson would register them for
        // reflection, which makes the native build considerably slower and the native executable larger
        Set<String> openAIClassNames = new HashSet<>();
        PathFilter pathFilter = PathFilter.forIncludes(Set.of("com/openai/**/*.class"));
        curateOutcome.getApplicationModel()
                .getRuntimeDependencies()
                .stream()
                .filter(dependency -> dependency.getGroupId().equals("com.openai")
                        && dependency.getArtifactId().equals("openai-java-core"))
                .forEach(dependency -> dependency.getContentTree(pathFilter).walk(visit -> {
                    String className = StringHelper.before(visit.getResourceName(), ".class").replace('/', '.');
                    if (isRequiredClass(className)) {
                        openAIClassNames.add(className);
                    }
                }));

        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(openAIClassNames.toArray(new String[0])));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerMcpSdkServiceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        // the MCP client (mcpServer.* endpoint options) resolves its JSON mapper and schema
        // validator through java.util.ServiceLoader; without these registrations a native
        // application fails at runtime with "No McpJsonMapperSupplier available"
        serviceProvider.produce(new ServiceProviderBuildItem(
                "io.modelcontextprotocol.json.McpJsonMapperSupplier",
                "io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapperSupplier"));
        serviceProvider.produce(new ServiceProviderBuildItem(
                "io.modelcontextprotocol.json.schema.JsonSchemaValidatorSupplier",
                "io.modelcontextprotocol.json.schema.jackson2.JacksonJsonSchemaValidatorSupplier"));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerMcpSdkSchemaForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // the MCP protocol messages (McpSchema records and friends) are (de)serialized by Jackson
        // reflectively; the MCP Java SDK ships no native-image metadata
        Set<String> mcpSchemaClassNames = combinedIndex.getIndex()
                .getKnownClasses()
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .filter(className -> className.startsWith("io.modelcontextprotocol.spec")
                        || className.startsWith("io.modelcontextprotocol.json"))
                .collect(Collectors.toSet());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(mcpSchemaClassNames.toArray(new String[0]))
                .constructors()
                .fields()
                .methods()
                .build());
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerForReflection(
            Capabilities capabilities,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<IgnoreJsonDeserializeClassBuildItem> ignoredJsonDeserializeClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<NativeImageResourcePatternsBuildItem> nativeResourcePatterns) {

        reflectiveHierarchy.produce(ReflectiveHierarchyBuildItem
                .builder(Type.create(ChatCompletion.class)).ignoreNested(false)
                .build());

        reflectiveHierarchy.produce(ReflectiveHierarchyBuildItem
                .builder(Type.create(ChatCompletionChunk.class)).ignoreNested(false)
                .build());

        // Make quarkus-kotlin optional since not everything it provides is required
        if (capabilities.isMissing(Capability.KOTLIN)) {
            Stream.of(JsonField.class.getName(), JsonValue.class.getName())
                    .map(DotName::createSimple)
                    .forEach(className -> {
                        // Suppress quarkus-jackson adding its own reflective config for JsonDeserialize so we can add our own
                        ignoredJsonDeserializeClass.produce(new IgnoreJsonDeserializeClassBuildItem(className));
                        reflectiveHierarchy.produce(ReflectiveHierarchyBuildItem
                                .builder(Type.create(className, Type.Kind.CLASS)).ignoreNested(false)
                                .build());
                    });

            Set<String> openAIModelClassNames = combinedIndex.getIndex()
                    .getKnownClasses()
                    .stream()
                    .map(ClassInfo::name)
                    .map(DotName::toString)
                    .filter(className -> className.startsWith(OPENAI_MODELS_PACKAGE))
                    .collect(Collectors.toSet());

            reflectiveClass.produce(ReflectiveClassBuildItem.builder(openAIModelClassNames.toArray(new String[0]))
                    .fields()
                    .methods()
                    .build());

            nativeResourcePatterns.produce(NativeImageResourcePatternsBuildItem.builder()
                    .includeGlobs("META-INF/**/*.kotlin_module",
                            "META-INF/services/kotlin.reflect.*",
                            "**/*.kotlin_builtins")
                    .build());
        }
    }

    private static boolean isRequiredClass(String className) {
        if (!className.startsWith(OPENAI_MODELS_PACKAGE)) {
            return true;
        }
        String relativeName = className.substring(OPENAI_MODELS_PACKAGE.length());
        int idx = relativeName.indexOf('.');
        // Classes that are directly within com.openai.models are shared across the sub packages
        return idx == -1 || OPENAI_MODELS_SUB_PACKAGES.contains(relativeName.substring(0, idx));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    RemovedResourceBuildItem excludeNativeImageDirectives() {
        // Remove all native-image directives from openai-java-core as it's mostly redundant & inaccurate for Quarkus
        return new RemovedResourceBuildItem(
                ArtifactKey.fromString("com.openai:openai-java-core"),
                Set.of(
                        "META-INF/native-image/jni-config.json",
                        "META-INF/native-image/predefined-classes-config.json",
                        "META-INF/native-image/proxy-config.json",
                        "META-INF/native-image/reflect-config.json",
                        "META-INF/native-image/resource-config.json",
                        "META-INF/native-image/serialization-config.json"));
    }
}
