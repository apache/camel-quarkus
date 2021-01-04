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
package org.apache.camel.quarkus.component.minio.deployment;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import io.minio.BaseArgs;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class MinioProcessor {

    private static final String FEATURE = "camel-minio";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        List<String> dtos = new LinkedList<>();
        dtos.addAll(index.getAllKnownSubclasses(DotName.createSimple(BaseArgs.class.getName())).stream()
                .map(c -> c.name().toString()).collect(Collectors.toList()));
        dtos.addAll(index.getAllKnownImplementors(DotName.createSimple("org.simpleframework.xml.core.Label")).stream()
                .map(c -> c.name().toString()).collect(Collectors.toList()));

        return new ReflectiveClassBuildItem(false, false, dtos.toArray(new String[dtos.size()]));
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflectionWithFields(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        String[] dtosWithFields = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("io.minio.messages"))
                .toArray(String[]::new);

        return new ReflectiveClassBuildItem(false, true, dtosWithFields);
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.minio", "minio"));
        indexDependency.produce(new IndexDependencyBuildItem("com.carrotsearch.thirdparty", "simple-xml-safe"));
    }
}
