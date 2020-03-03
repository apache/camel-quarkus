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

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;

class IcalProcessor {

    private static final String FEATURE = "camel-ical";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void nativeResources(
            BuildProducer<NativeImageResourceBuildItem> nativeResources,
            BuildProducer<NativeImageResourceDirectoryBuildItem> nativeResourceDirs) {

        nativeResources.produce(new NativeImageResourceBuildItem("net/fortuna/ical4j/model/tz.alias"));

        Stream.of("zoneinfo/Africa",
                "zoneinfo/America",
                "zoneinfo/America/Argentina",
                "zoneinfo/America/Indiana",
                "zoneinfo/America/Kentucky",
                "zoneinfo/America/North_Dakota",
                "zoneinfo/Antarctica",
                "zoneinfo/Arctic",
                "zoneinfo/Asia",
                "zoneinfo/Atlantic",
                "zoneinfo/Australia",
                "zoneinfo/Europe",
                "zoneinfo/Indian",
                "zoneinfo/Pacific")
                .forEach(path -> nativeResourceDirs.produce(new NativeImageResourceDirectoryBuildItem(path)));
    }

}
