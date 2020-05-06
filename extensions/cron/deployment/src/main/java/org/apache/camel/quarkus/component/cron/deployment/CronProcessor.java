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
package org.apache.camel.quarkus.component.cron.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.component.cron.api.CamelCronConfiguration;
import org.apache.camel.quarkus.core.deployment.CamelServicePatternBuildItem;
import org.apache.camel.quarkus.core.deployment.UnbannedReflectiveBuildItem;

class CronProcessor {

    private static final String FEATURE = "camel-cron";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    UnbannedReflectiveBuildItem whitelistConfigurationClasses() {
        return new UnbannedReflectiveBuildItem(CamelCronConfiguration.class.getName());
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(true, false, CamelCronConfiguration.class);
    }

    @BuildStep
    CamelServicePatternBuildItem camelCronServicePattern() {
        return new CamelServicePatternBuildItem(CamelServicePatternBuildItem.CamelServiceDestination.DISCOVERY, true,
                "META-INF/services/org/apache/camel/cron/*");
    }
}
