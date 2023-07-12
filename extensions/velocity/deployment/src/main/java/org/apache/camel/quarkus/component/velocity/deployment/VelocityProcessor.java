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
package org.apache.camel.quarkus.component.velocity.deployment;

import java.util.ArrayList;
import java.util.TreeMap;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.component.velocity.CamelVelocityClasspathResourceLoader;
import org.apache.velocity.runtime.directive.ForeachScope;
import org.jboss.jandex.IndexView;

import static java.util.stream.Collectors.toCollection;

class VelocityProcessor {

    private static final String FEATURE = "camel-velocity";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceBuildItem initResources() {
        return new NativeImageResourceBuildItem(
                "org/apache/velocity/runtime/defaults/velocity.properties",
                "org/apache/velocity/runtime/defaults/directive.properties");
    }

    @BuildStep
    void reflectiveClass(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        ArrayList<String> dtos = index.getKnownClasses().stream().map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("org.apache.velocity.runtime") ||
                        n.startsWith("org.apache.velocity.util.introspection"))
                .collect(toCollection(ArrayList::new));

        dtos.add(CamelVelocityClasspathResourceLoader.class.getName());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(dtos.toArray(new String[dtos.size()])).build());

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(
                        TreeMap.class.getName(),
                        ForeachScope.class.getName())
                        .methods()
                        .build());
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("org.apache.velocity", "velocity-engine-core");
    }
}
