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

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class SupportLangchain4jProcessor {

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexedDependencies) {
        indexedDependencies.produce(new IndexDependencyBuildItem("dev.langchain4j", "langchain4j-http-client-jdk"));
        indexedDependencies.produce(new IndexDependencyBuildItem("dev.langchain4j", "langchain4j-ollama"));
    }

    @BuildStep
    ServiceProviderBuildItem registerServiceProviders() {
        return ServiceProviderBuildItem.allProvidersFromClassPath("dev.langchain4j.http.client.HttpClientBuilderFactory");
    }

    @BuildStep
    void registerForReflection(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Set<String> ollamaModelClasses = combinedIndex.getIndex()
                .getClassesInPackage("dev.langchain4j.model.ollama")
                .stream()
                .filter(classInfo -> classInfo.annotations().stream()
                        .anyMatch(annotationInstance -> annotationInstance.name().toString()
                                .startsWith("com.fasterxml.jackson.annotation")))
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toSet());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(ollamaModelClasses.toArray(new String[0]))
                .methods(true)
                .build());
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClasses() {
        return new RuntimeInitializedClassBuildItem("dev.langchain4j.internal.RetryUtils");
    }
}
