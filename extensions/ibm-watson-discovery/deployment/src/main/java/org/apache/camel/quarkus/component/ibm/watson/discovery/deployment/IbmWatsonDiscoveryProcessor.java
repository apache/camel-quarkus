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
package org.apache.camel.quarkus.component.ibm.watson.discovery.deployment;

import com.ibm.cloud.sdk.core.security.IamToken;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.ClassInfo;

/**
 * Quarkus extension processor for IBM Watson Discovery support.
 */
class IbmWatsonDiscoveryProcessor {

    private static final String FEATURE = "camel-ibm-watson-discovery";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(IamToken.class.getName())
                .methods().constructors().fields().build());

        for (ClassInfo classInfo : combinedIndex.getIndex().getKnownClasses()) {
            String className = classInfo.name().toString();
            if (className.startsWith("com.ibm.watson.discovery.v2.model.")) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(className)
                        .methods().constructors().fields().build());
            }
        }
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("com.ibm.watson", "discovery"));
    }

    @BuildStep
    void addResources(BuildProducer<NativeImageResourceBuildItem> nativeImageResource) {
        nativeImageResource.produce(new NativeImageResourceBuildItem("java-sdk-version.properties"));
        nativeImageResource.produce(new NativeImageResourceBuildItem("sdk-core-version.properties"));
    }
}
