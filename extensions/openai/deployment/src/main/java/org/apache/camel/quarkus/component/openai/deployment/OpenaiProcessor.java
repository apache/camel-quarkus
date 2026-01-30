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
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.jackson.deployment.IgnoreJsonDeserializeClassBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

class OpenaiProcessor {
    private static final String FEATURE = "camel-openai";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    IndexDependencyBuildItem indexDependencies() {
        return new IndexDependencyBuildItem("com.openai", "openai-java-core");
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
                    .filter(className -> className.startsWith("com.openai.models"))
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
