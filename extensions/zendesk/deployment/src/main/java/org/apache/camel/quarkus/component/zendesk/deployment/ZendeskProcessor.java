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
package org.apache.camel.quarkus.component.zendesk.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.jboss.jandex.IndexView;

class ZendeskProcessor {

    private static final String FEATURE = "camel-zendesk";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem boxArchiveMarker() {
        return new AdditionalApplicationArchiveMarkerBuildItem("org/zendesk");
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        String[] zendeskModelClasses = index.getKnownClasses()
                .stream()
                .filter(CamelSupport::isConcrete)
                .map(classInfo -> classInfo.name().toString())
                .filter(className -> className.startsWith("org.zendesk.client.v2.model"))
                .toArray(String[]::new);

        return ReflectiveClassBuildItem.builder(zendeskModelClasses).methods().build();
    }
}
