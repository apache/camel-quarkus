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
package org.apache.camel.quarkus.component.geocoder.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class GeocoderProcessor {

    private static final String FEATURE = "camel-geocoder";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerReflectiveClasses() {
        List<ReflectiveClassBuildItem> items = new ArrayList<ReflectiveClassBuildItem>();
        items.add(
                ReflectiveClassBuildItem.builder("com.google.maps.GeocodingApi$Response").methods(false).fields(true).build());
        items.add(
                ReflectiveClassBuildItem.builder("com.google.maps.model.GeocodingResult").methods(false).fields(true).build());
        items.add(
                ReflectiveClassBuildItem.builder("com.google.maps.model.AddressComponent").methods(false).fields(true).build());
        items.add(ReflectiveClassBuildItem.builder("com.google.maps.model.Geometry").methods(false).fields(true).build());
        items.add(ReflectiveClassBuildItem.builder("com.google.maps.model.AddressType").methods(false).fields(true).build());
        items.add(ReflectiveClassBuildItem.builder("com.google.maps.model.PlusCode").methods(false).fields(true).build());
        items.add(ReflectiveClassBuildItem.builder("com.google.maps.model.Bounds").methods(false).fields(true).build());
        items.add(ReflectiveClassBuildItem.builder("com.google.maps.model.LatLng").methods(false).fields(true).build());
        items.add(ReflectiveClassBuildItem.builder("com.google.maps.model.LocationType").methods(false).fields(true).build());
        items.add(ReflectiveClassBuildItem.builder("com.google.maps.model.GeolocationPayload").methods(false).fields(true)
                .build());
        return items;
    }
}
