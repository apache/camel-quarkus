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
package org.apache.camel.quarkus.component.cxf.soap.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class CxfSoapProcessor {

    private static final String FEATURE = "camel-cxf-soap";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of(
                "org.apache.cxf.attachment.AttachmentUtil")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        Stream.of(
                "org.apache.cxf.feature.AbstractFeature",
                "org.apache.wss4j.dom.handler.WSHandler",
                "org.apache.cxf.phase.AbstractPhaseInterceptor",
                "org.apache.cxf.binding.soap.interceptor.AbstractSOAPInterceptor",
                "org.apache.cxf.phase.PhaseInterceptor")
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownSubclasses(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClass::produce);

        Stream.of(
                "org.apache.cxf.feature.Feature",
                "org.apache.cxf.interceptor.Interceptor",
                "org.apache.cxf.binding.soap.interceptor.SoapInterceptor",
                "org.apache.cxf.phase.PhaseInterceptor")
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClass::produce);
    }
}
