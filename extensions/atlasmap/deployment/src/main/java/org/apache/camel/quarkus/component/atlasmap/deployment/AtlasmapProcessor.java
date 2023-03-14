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
package org.apache.camel.quarkus.component.atlasmap.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import io.atlasmap.core.DefaultAtlasContextFactory;
import io.atlasmap.core.DefaultAtlasModuleInfo;
import io.atlasmap.csv.module.CsvModule;
import io.atlasmap.java.module.JavaModule;
import io.atlasmap.json.module.JsonModule;
import io.atlasmap.mxbean.AtlasContextFactoryMXBean;
import io.atlasmap.mxbean.AtlasModuleInfoMXBean;
import io.atlasmap.spi.AtlasConverter;
import io.atlasmap.spi.AtlasFieldAction;
import io.atlasmap.v2.Action;
import io.atlasmap.v2.DataSourceMetadata;
import io.atlasmap.xml.module.XmlModule;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import org.jboss.jandex.IndexView;

class AtlasmapProcessor {

    private static final String FEATURE = "camel-atlasmap";
    private static final String ATLASMAP_SERVICE_BASE = "META-INF/services/";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerReflectiveClasses() {
        List<ReflectiveClassBuildItem> items = new ArrayList<>();
        items.add(new ReflectiveClassBuildItem(false, false, DefaultAtlasContextFactory.class));
        items.add(new ReflectiveClassBuildItem(false, false, DefaultAtlasModuleInfo.class));
        items.add(new ReflectiveClassBuildItem(true, false, JsonModule.class));
        items.add(new ReflectiveClassBuildItem(true, false, CsvModule.class));
        items.add(new ReflectiveClassBuildItem(true, false, JavaModule.class));
        items.add(new ReflectiveClassBuildItem(true, false, XmlModule.class));
        items.add(new ReflectiveClassBuildItem(false, true, false, AtlasContextFactoryMXBean.class));
        items.add(new ReflectiveClassBuildItem(false, true, false, AtlasModuleInfoMXBean.class));
        items.add(new ReflectiveClassBuildItem(false, true, false, DataSourceMetadata.class));
        return items;
    }

    @BuildStep
    void nativeImageResources(BuildProducer<NativeImageResourceBuildItem> nativeImageResource) {
        nativeImageResource.produce(new NativeImageResourceBuildItem("META-INF/services/atlas/module/atlas.module"));
        nativeImageResource.produce(new NativeImageResourceBuildItem("atlasmap.properties"));
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();
        // register Atlasmap model classes for serialize/deserialize
        String[] dtos = index.getKnownClasses().stream().map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("io.atlasmap.v2")
                        || n.startsWith("io.atlasmap.json.v2")
                        || n.startsWith("io.atlasmap.java.v2")
                        || n.startsWith("io.atlasmap.xml.v2")
                        || n.startsWith("io.atlasmap.csv.v2")
                        || n.startsWith("io.atlasmap.dfdl.v2"))
                .toArray(String[]::new);
        return new ReflectiveClassBuildItem(true, false, dtos);
    }

    @BuildStep
    void registerNativeImageResources(BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Stream.of(
                AtlasConverter.class.getName(),
                AtlasFieldAction.class.getName(),
                Action.class.getName())
                .forEach(service -> {
                    try {
                        Set<String> implementations = ServiceUtil.classNamesNamedIn(
                                Thread.currentThread().getContextClassLoader(),
                                ATLASMAP_SERVICE_BASE + service);
                        services.produce(
                                new ServiceProviderBuildItem(service,
                                        implementations.toArray(new String[0])));

                        // register those classes for reflection too
                        // we don't need to add external dependency atlas-core for the services
                        String[] dtos = implementations.stream()
                                .toArray(String[]::new);
                        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, dtos));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
