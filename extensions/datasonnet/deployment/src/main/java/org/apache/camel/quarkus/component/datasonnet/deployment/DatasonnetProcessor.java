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
package org.apache.camel.quarkus.component.datasonnet.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.camel.language.datasonnet.DatasonnetLanguage;
import org.apache.camel.quarkus.component.datasonnet.DatasonnetLanguageRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;

class DatasonnetProcessor {

    private static final String FEATURE = "camel-datasonnet";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("com.datasonnet", "datasonnet-mapper"));
        indexDependency.produce(new IndexDependencyBuildItem("org.scala-lang", "scala-library"));
        indexDependency.produce(new IndexDependencyBuildItem("org.scala-lang.modules", "scala-collection-compat_2.13"));
    }

    @BuildStep
    DatasonnetLibrariesBuildItem discoverDatasonnetLibraries(ApplicationArchivesBuildItem applicationArchivesBuildItem) {
        Map<String, String> datasonnetLibraries = new HashMap<>();
        for (ApplicationArchive archive : applicationArchivesBuildItem.getAllApplicationArchives()) {
            for (Path root : archive.getRootDirectories()) {
                try (Stream<Path> files = Files.walk(root)) {
                    files.filter(Files::isRegularFile)
                            .map(root::relativize)
                            .filter(resource -> resource.toString().endsWith(".libsonnet"))
                            .forEach(resource -> {
                                try {
                                    datasonnetLibraries.put(resource.toString(), Files.readString(resource));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                } catch (IOException e) {
                    throw new RuntimeException("Could not walk " + root, e);
                }
            }
        }
        return new DatasonnetLibrariesBuildItem(datasonnetLibraries);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem createDatasonnetLanguage(DatasonnetLibrariesBuildItem datasonnetLibraries,
            DatasonnetLanguageRecorder recorder) {
        return new CamelBeanBuildItem("datasonnet", DatasonnetLanguage.class.getName(),
                recorder.createDatasonnetLanguage(datasonnetLibraries.getLibraries()));
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of("scala.util.Random$")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

    @BuildStep
    void nativeImageResources(
            DatasonnetLibrariesBuildItem datasonnetLibraries,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResource) {
        datasonnetLibraries.getLibraryPaths()
                .stream()
                .map(NativeImageResourceBuildItem::new)
                .forEach(nativeImageResource::produce);
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerForReflection() {
        List<ReflectiveClassBuildItem> items = new ArrayList<>();
        items.add(ReflectiveClassBuildItem.builder("com.datasonnet.jsonnet.Expr[]").methods(true).fields(false).build());
        items.add(ReflectiveClassBuildItem.builder("com.datasonnet.jsonnet.Expr$Member$Field[]").methods(true).fields(false)
                .build());
        return items;
    }
}
