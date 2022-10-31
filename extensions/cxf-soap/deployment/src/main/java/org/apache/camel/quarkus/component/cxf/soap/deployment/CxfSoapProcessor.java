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

import io.quarkiverse.cxf.deployment.CxfRouteRegistrationRequestorBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class CxfSoapProcessor {

    private static final String FEATURE = "camel-cxf-soap";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    SystemPropertyBuildItem ehcacheAgentSizeOfBypass() {
        return new SystemPropertyBuildItem("org.ehcache.sizeof.AgentSizeOf.bypass", "true");
    }

    @BuildStep
    void registerForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods) {

        IndexView index = combinedIndex.getIndex();

        Stream.of(
                "org.apache.wss4j.dom.handler.WSHandler") // can we remove this?
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownSubclasses(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClass::produce);

        reflectiveMethods.produce(new ReflectiveMethodBuildItem("org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory",
                "getServiceFactory", new String[0]));

    }

    @BuildStep
    CxfRouteRegistrationRequestorBuildItem requestCxfRouteRegistration() {
        return new CxfRouteRegistrationRequestorBuildItem(FEATURE);
    }
}
