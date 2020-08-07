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
package org.apache.camel.quarkus.component.box.deployment;

import com.box.sdk.BoxAPIRequest;
import com.box.sdk.BoxDeveloperEditionAPIConnection;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class BoxProcessor {

    private static final String FEATURE = "camel-box";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem boxArchiveMarker() {
        return new AdditionalApplicationArchiveMarkerBuildItem("com/box/sdk");
    }

    @BuildStep
    void configure(BuildProducer<RuntimeInitializedClassBuildItem> runtimeClasses) {
        // Avoid use of sun.security.provider.NativePRNG at image runtime
        runtimeClasses.produce(new RuntimeInitializedClassBuildItem(BoxDeveloperEditionAPIConnection.class.getCanonicalName()));
        runtimeClasses.produce(new RuntimeInitializedClassBuildItem(BoxAPIRequest.class.getCanonicalName()));
    }
}
