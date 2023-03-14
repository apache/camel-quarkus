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
package org.apache.camel.quarkus.component.servicenow.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.component.servicenow.ServiceNowExceptionModel;
import org.apache.camel.component.servicenow.annotations.ServiceNowSysParm;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class ServicenowProcessor {

    private static final String FEATURE = "camel-servicenow";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        // Get candidate DTOs annotated with ServiceNowSysParm
        String[] serviceNowDtos = index.getAnnotations(DotName.createSimple(ServiceNowSysParm.class.getName()))
                .stream()
                .map(AnnotationInstance::target)
                .filter(annotationTarget -> annotationTarget.kind().equals(AnnotationTarget.Kind.CLASS))
                .map(AnnotationTarget::asClass)
                .filter(CamelSupport::isConcrete)
                .filter(CamelSupport::isPublic)
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> !className.startsWith("org.apache.camel.component.servicenow.model"))
                .toArray(String[]::new);

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(serviceNowDtos).methods(false).fields(true).build());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(HTTPTransportFactory.class,
                ServiceNowExceptionModel.class).methods(false).fields(true).build());
    }
}
