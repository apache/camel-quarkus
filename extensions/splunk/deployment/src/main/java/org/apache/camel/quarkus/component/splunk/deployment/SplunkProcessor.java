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
package org.apache.camel.quarkus.component.splunk.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.splunk.HttpService;
import com.splunk.Index;
import com.splunk.Input;
import com.splunk.SavedSearch;
import com.splunk.Service;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.joda.time.DateTimeZone;

class SplunkProcessor {

    private static final String FEATURE = "camel-splunk";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitBcryptUtil() {
        // this class uses a SecureRandom which needs to be initialised at run time
        return new RuntimeInitializedClassBuildItem(HttpService.class.getName());
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        List<String> dtos = index.getAllKnownSubclasses(DotName.createSimple(Input.class))
                .stream()
                .map(c -> c.name().toString())
                .toList();

        return ReflectiveClassBuildItem.builder(dtos.toArray(new String[0])).build();
    }

    @BuildStep
    List<ReflectiveClassBuildItem> reflectiveClasses() {
        return Arrays.asList(ReflectiveClassBuildItem.builder(Index.class.getName()).constructors().build(),
                ReflectiveClassBuildItem.builder(SavedSearch.class.getName()).constructors().build(),
                ReflectiveClassBuildItem.builder(Input.class.getName()).constructors().build(),
                ReflectiveClassBuildItem.builder(Service.class.getName()).constructors().build());
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.splunk");
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResources() {
        List<String> timezones = new ArrayList<>();
        for (String timezone : DateTimeZone.getAvailableIDs()) {
            String[] zoneParts = timezone.split("/");
            if (zoneParts.length == 2) {
                timezones.add(String.format("org/joda/time/tz/data/%s/%s", zoneParts[0], zoneParts[1]));
            }
        }
        return new NativeImageResourceBuildItem(timezones);
    }

}
