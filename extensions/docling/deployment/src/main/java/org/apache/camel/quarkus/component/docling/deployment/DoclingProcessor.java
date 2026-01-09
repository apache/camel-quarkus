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
package org.apache.camel.quarkus.component.docling.deployment;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class DoclingProcessor {

    private static final String FEATURE = "camel-docling";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexedDependency) {
        indexedDependency.produce(new IndexDependencyBuildItem("ai.docling", "docling-core"));
        indexedDependency.produce(new IndexDependencyBuildItem("ai.docling", "docling-serve-api"));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        // Register Docling model and associated builder classes for reflection for Jackson serialization / deserialization
        Set<String> doclingValidationBuilderClasses = combinedIndex.getIndex()
                .getClassesInPackage("ai.docling.serve.api.validation")
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .filter(className -> className.endsWith("$Builder"))
                .collect(Collectors.toUnmodifiableSet());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(doclingValidationBuilderClasses.toArray(new String[0]))
                .methods(true)
                .build());

        Set<String> doclingCoreBuilderClasses = combinedIndex.getIndex()
                .getClassesInPackage("ai.docling.core")
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .filter(className -> className.endsWith("$Builder"))
                .collect(Collectors.toUnmodifiableSet());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(doclingCoreBuilderClasses.toArray(new String[0]))
                .methods(true)
                .build());
    }
}
