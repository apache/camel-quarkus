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
package org.apache.camel.quarkus.support.google.http.client.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class SupportGoogleHTTPClientProcessor {

    private static final String FEATURE = "camel-support-google-http-client";

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem applicationArchiveMarkers(
            BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> applicationArchiveMarker) {
        return new AdditionalApplicationArchiveMarkerBuildItem("com/google/api/client");
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClasses() {
        return new RuntimeInitializedClassBuildItem("com.google.api.client.auth.oauth.OAuthParameters");
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        // Google client API class reflection for JSON serialization / deserialization
        String[] googleApis = index.getAllKnownSubclasses(DotName.createSimple("com.google.api.client.util.GenericData"))
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, googleApis));
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }
}
