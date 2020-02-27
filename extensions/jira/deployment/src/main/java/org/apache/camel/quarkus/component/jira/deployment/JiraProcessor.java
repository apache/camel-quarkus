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
package org.apache.camel.quarkus.component.jira.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.camel.component.jira.JiraConfiguration;
import org.apache.camel.quarkus.core.deployment.UnbannedReflectiveBuildItem;
import org.joda.time.DateTimeZone;

class JiraProcessor {

    private static final String FEATURE = "camel-jira";

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem configureRuntimeInitializedClasses() {
        return new RuntimeInitializedClassBuildItem("com.google.api.client.auth.oauth.OAuthParameters");
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(true, true, JiraConfiguration.class);
    }

    @BuildStep()
    UnbannedReflectiveBuildItem unbannedReflectiveItems() {
        return new UnbannedReflectiveBuildItem(JiraConfiguration.class.getName());
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResources() {
        // Add Joda timezone resources into the native image as it is required by com.atlassian.jira.rest.client.internal.json.JsonParseUtil
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
