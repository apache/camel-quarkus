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
package org.apache.camel.quarkus.component.caffeine.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;

import static io.quarkus.caffeine.runtime.graal.CacheConstructorsFeature.REGISTER_RECORD_STATS_IMPLEMENTATIONS;

class CaffeineProcessor {
    private static final String FEATURE = "camel-caffeine";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = CamelCaffeineStatsEnabled.class)
    NativeImageSystemPropertyBuildItem registerRecordStatsImplementations() {
        return new NativeImageSystemPropertyBuildItem(REGISTER_RECORD_STATS_IMPLEMENTATIONS, "true");
    }

    static final class CamelCaffeineStatsEnabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return CamelSupport.getOptionalConfigValue("camel.component.caffeine-cache.stats-enabled", boolean.class, false) ||
                    CamelSupport.getOptionalConfigValue("camel.component.caffeine-cache.statsEnabled", boolean.class, false);
        }
    }
}
