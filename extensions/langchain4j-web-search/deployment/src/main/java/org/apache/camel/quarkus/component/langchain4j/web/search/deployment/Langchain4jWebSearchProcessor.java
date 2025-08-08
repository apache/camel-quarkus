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
package org.apache.camel.quarkus.component.langchain4j.web.search.deployment;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class Langchain4jWebSearchProcessor {
    private static final String FEATURE = "camel-langchain4j-web-search";
    private static final Pattern WEB_SEARCH_API_CLASS_PATTERN = Pattern.compile(".*(Response|SearchRequest|SearchResult)");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void indexDependencies(
            BuildProducer<IndexDependencyBuildItem> indexedDependency,
            CurateOutcomeBuildItem curateOutcome) {

        // Index any dependencies with artifactId prefix langchain4j-web-search-engine
        ApplicationModel applicationModel = curateOutcome.getApplicationModel();
        for (ResolvedDependency dependency : applicationModel.getDependencies()) {
            if (dependency.getGroupId().equals("dev.langchain4j")
                    && dependency.getArtifactId().startsWith("langchain4j-web-search-engine")) {
                String artifactId = dependency.getArtifactId();
                indexedDependency.produce(new IndexDependencyBuildItem(dependency.getGroupId(), artifactId));
            }
        }
    }

    @BuildStep
    NativeImageProxyDefinitionBuildItem registerRestServiceProxies(CombinedIndexBuildItem combinedIndex) {
        // If there are any retrofit2 REST service definitions, we need to register native proxy definitions for them
        Set<String> restServiceClasses = combinedIndex.getIndex()
                .getSubpackages("dev.langchain4j.web.search")
                .stream()
                .flatMap(thePackage -> combinedIndex.getIndex().getClassesInPackage(thePackage).stream())
                .map(ClassInfo::asClass)
                .filter(classInfo -> classInfo.annotations().stream().anyMatch(
                        annotationInstance -> annotationInstance.name().toString().startsWith("retrofit2.http")))
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toSet());
        return new NativeImageProxyDefinitionBuildItem(restServiceClasses.toArray(new String[0]));
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        Set<String> webSearchApiClasses = combinedIndex.getIndex()
                .getSubpackages("dev.langchain4j.web.search")
                .stream()
                .flatMap(thePackage -> combinedIndex.getIndex().getClassesInPackage(thePackage).stream())
                .map(ClassInfo::asClass)
                .map(ClassInfo::name)
                .map(DotName::toString)
                .filter(className -> WEB_SEARCH_API_CLASS_PATTERN.matcher(className).matches())
                .collect(Collectors.toSet());

        return ReflectiveClassBuildItem
                .builder(webSearchApiClasses.toArray(new String[0]))
                .fields(true)
                .methods(true).build();
    }
}
