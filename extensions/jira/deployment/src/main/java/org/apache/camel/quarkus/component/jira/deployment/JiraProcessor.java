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
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.IndexView;
import org.joda.time.DateTimeZone;

class JiraProcessor {

    private static final String FEATURE = "camel-jira";
    private static final String JIRA_MODEL_PACKAGE = "com.atlassian.jira.rest.client.api.domain";

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
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

    @BuildStep
    ReflectiveClassBuildItem registerJiraClassesForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();
        String[] modelClasses = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> n.startsWith(JIRA_MODEL_PACKAGE))
                .toArray(String[]::new);
        return new ReflectiveClassBuildItem(true, false, modelClasses);
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("com.atlassian.jira", "jira-rest-java-client-api");
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerReflectiveClasses() {
        List<ReflectiveClassBuildItem> items = new ArrayList<>();
        items.add(new ReflectiveClassBuildItem(true, false, "com.atlassian.jira.rest.client.api.StatusCategory"));
        items.add(new ReflectiveClassBuildItem(true, false, "org.codehaus.jettison.json.JSONArray"));
        items.add(new ReflectiveClassBuildItem(true, false, "org.codehaus.jettison.json.JSONObject"));
        return items;
    }
}
