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
package org.apache.camel.quarkus.component.couchdb.deployment;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class CouchdbProcessor {

    private static final String FEATURE = "camel-couchdb";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerReflectiveClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Set<String> modelClasses = combinedIndex.getIndex()
                .getClassesInPackage(DotName.createSimple("com.ibm.cloud.cloudant.v1.model"))
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toUnmodifiableSet());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(modelClasses.toArray(new String[0]))
                .fields(true)
                .build());
    }

    @BuildStep
    void addDependenciesToIndexer(BuildProducer<IndexDependencyBuildItem> indexDependencyProducer) {
        indexDependencyProducer.produce(new IndexDependencyBuildItem("com.ibm.cloud", "cloudant"));
    }

}
