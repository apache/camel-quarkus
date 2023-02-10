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
package org.apache.camel.quarkus.component.twilio.deployment;

import java.util.stream.Stream;

import com.twilio.base.Creator;
import com.twilio.base.Deleter;
import com.twilio.base.Fetcher;
import com.twilio.base.Reader;
import com.twilio.base.Updater;
import com.twilio.type.Endpoint;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class TwilioProcessor {

    private static final String FEATURE = "camel-twilio";

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
        return new AdditionalApplicationArchiveMarkerBuildItem("com/twilio");
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        // Register Twilio API CRUD generator classes for reflection
        String[] reflectiveClasses = Stream.of(Creator.class, Deleter.class, Fetcher.class, Reader.class, Updater.class)
                .map(Class::getName)
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownSubclasses(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> className.startsWith("com.twilio.rest.api.v2010"))
                .toArray(String[]::new);
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, reflectiveClasses));

        // Register Twilio Endpoint implementors for reflection
        String[] endpointImplementors = index.getAllKnownImplementors(DotName.createSimple(Endpoint.class.getName()))
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, endpointImplementors));
    }
}
