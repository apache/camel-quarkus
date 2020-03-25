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
import org.apache.camel.component.servicenow.ServiceNowConfiguration;
import org.apache.camel.quarkus.core.deployment.UnbannedReflectiveBuildItem;
import org.apache.cxf.transport.http.HTTPTransportFactory;

class ServicenowProcessor {

    private static final String FEATURE = "camel-servicenow";

    private static final String[] reflectionClasses = new String[] {
            ServiceNowConfiguration.class.getCanonicalName(),
            HTTPTransportFactory.class.getCanonicalName()
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<UnbannedReflectiveBuildItem> unbannedClass, CombinedIndexBuildItem combinedIndex) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, reflectionClasses));
        unbannedClass.produce(new UnbannedReflectiveBuildItem(reflectionClasses));
    }
}
