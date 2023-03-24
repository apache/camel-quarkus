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
package org.apache.camel.quarkus.component.stax.deployment;

import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class StaxProcessor {

    private static final String FEATURE = "camel-stax";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem contributeClassesToIndex() {
        return new AdditionalIndexedClassesBuildItem(DefaultHandler.class.getName());
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        // Register any ContentHandler impls for reflection so they can be used by the sax component
        IndexView index = combinedIndex.getIndex();
        String[] contentHandlers = index.getAllKnownImplementors(DotName.createSimple(ContentHandler.class.getName()))
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .toArray(String[]::new);
        return ReflectiveClassBuildItem.builder(contentHandlers).build();
    }
}
