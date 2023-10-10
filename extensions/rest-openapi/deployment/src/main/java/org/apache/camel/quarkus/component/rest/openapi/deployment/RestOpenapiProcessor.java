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
package org.apache.camel.quarkus.component.rest.openapi.deployment;

import java.util.ArrayList;
import java.util.List;

import com.github.fge.jsonschema.keyword.validator.KeywordValidator;
import com.github.fge.msgsimple.load.MessageBundleLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.swagger.v3.oas.models.media.Schema;
import org.jboss.jandex.IndexView;

class RestOpenapiProcessor {

    private static final String FEATURE = "camel-rest-openapi";
    private static final List<String> GROUP_IDS_TO_INDEX = List.of("com.github.java-json-tools", "com.atlassian.oai",
            "io.swagger.core.v3");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void indexDependencies(CurateOutcomeBuildItem curateOutcome, BuildProducer<IndexDependencyBuildItem> indexedDependency) {
        curateOutcome.getApplicationModel()
                .getDependencies()
                .stream()
                .filter(dependency -> GROUP_IDS_TO_INDEX.contains(dependency.getGroupId()))
                .map(dependency -> new IndexDependencyBuildItem(dependency.getGroupId(), dependency.getArtifactId()))
                .forEach(indexedDependency::produce);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        List<String> reflectiveClassNames = new ArrayList<>();
        IndexView index = combinedIndex.getIndex();

        index.getAllKnownImplementors(MessageBundleLoader.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .forEach(reflectiveClassNames::add);

        index.getAllKnownImplementors(KeywordValidator.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .forEach(reflectiveClassNames::add);

        index.getAllKnownSubclasses(Schema.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .forEach(reflectiveClassNames::add);

        index.getClassesInPackage("io.swagger.v3.core.jackson.mixin")
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .forEach(reflectiveClassNames::add);

        return ReflectiveClassBuildItem.builder(reflectiveClassNames.toArray(new String[0])).build();
    }

    @BuildStep
    void nativeImageResources(
            BuildProducer<NativeImageResourceDirectoryBuildItem> nativeImageResourceDirectory,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResource) {
        nativeImageResourceDirectory.produce(new NativeImageResourceDirectoryBuildItem("swagger/validation"));
        nativeImageResourceDirectory.produce(new NativeImageResourceDirectoryBuildItem("draftv3"));
        nativeImageResourceDirectory.produce(new NativeImageResourceDirectoryBuildItem("draftv4"));
        nativeImageResourceDirectory.produce(new NativeImageResourceDirectoryBuildItem("com/github/fge/jsonschema/validator"));
        nativeImageResource.produce(new NativeImageResourceBuildItem("com/github/fge/uritemplate/messages.properties"));
    }
}
