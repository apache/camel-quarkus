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
package org.apache.camel.quarkus.component.ical.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import net.fortuna.ical4j.model.TimeZoneRegistryImpl;
import net.fortuna.ical4j.util.MapTimeZoneCache;
import org.jboss.logging.Logger;

class IcalProcessor {
    private static final Logger LOG = Logger.getLogger(IcalProcessor.class);
    private static final String FEATURE = "camel-ical";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSSLNativeSupport() {
        // Required by TimeZoneUpdater$UrlBuilder.toUrl
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void nativeResources(
            BuildProducer<NativeImageResourceBuildItem> nativeResources,
            BuildProducer<NativeImageResourceDirectoryBuildItem> nativeResourceDirs) {

        nativeResources.produce(new NativeImageResourceBuildItem("net/fortuna/ical4j/model/tz.alias"));

        try (InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("net/fortuna/ical4j/model/tz.alias")) {
            Properties timezoneData = new Properties();
            timezoneData.load(stream);
            timezoneData.values()
                    .stream()
                    .map(Objects::toString)
                    .map(timeZone -> timeZone.split("/")[0])
                    .distinct()
                    .forEach(region -> {
                        nativeResourceDirs.produce(new NativeImageResourceDirectoryBuildItem("zoneinfo/" + region));
                        nativeResourceDirs.produce(new NativeImageResourceDirectoryBuildItem("zoneinfo-global/" + region));
                    });

        } catch (IOException e) {
            throw new RuntimeException("Failed reading ical tz.alias");
        }
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(
                        MapTimeZoneCache.class,
                        TimeZoneRegistryImpl.class).build());
    }
}
