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
package org.apache.camel.quarkus.component.salesforce.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.component.salesforce.api.dto.AbstractDTOBase;
import org.apache.camel.component.salesforce.internal.dto.PushTopic;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class SalesforceProcessor {

    private static final String FEATURE = "camel-salesforce";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        IndexView index = combinedIndex.getIndex();

        // Register everything extending AbstractDTOBase for reflection
        DotName dtoBaseName = DotName.createSimple(AbstractDTOBase.class.getName());
        String[] dtoClasses = index.getAllKnownSubclasses(dtoBaseName)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, dtoClasses));

        // Register internal DTO classes for reflection
        String[] internalDtoClasses = index.getKnownClasses()
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> className.startsWith("org.apache.camel.component.salesforce.internal.dto"))
                // it is registred below with fields accessible
                .filter(className -> className != PushTopic.class.getName())
                .toArray(String[]::new);

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, internalDtoClasses));

        // enabling the search for private fields : related to issue https://issues.apache.org/jira/browse/CAMEL-16860
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, PushTopic.class));
    }
}
