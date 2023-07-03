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
package org.apache.camel.quarkus.component.debug.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AllowJNDIBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.LaunchMode;
import org.apache.camel.quarkus.component.debug.DebugConfig;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.eclipse.microprofile.config.ConfigProvider;

class DebugProcessor {
    private static final String FEATURE = "camel-debug";

    @BuildStep(onlyIf = DebugEnabled.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = DebugEnabled.class)
    AllowJNDIBuildItem allowJNDI() {
        return new AllowJNDIBuildItem();
    }

    @BuildStep(onlyIfNot = DebugEnabled.class)
    CamelServicePatternBuildItem camelDebuggerFactoryServicePattern() {
        // Prevent debugging if not in dev mode. This is added as an exclusion since
        // core defines an include path filter for META-INF/services/org/apache/camel/*
        return new CamelServicePatternBuildItem(CamelServiceDestination.DISCOVERY, false,
                "META-INF/services/org/apache/camel/debugger-factory");
    }

    static class DebugEnabled implements BooleanSupplier {
        DebugConfig config;
        LaunchMode launchMode;

        @Override
        public boolean getAsBoolean() {
            return (launchMode.equals(LaunchMode.DEVELOPMENT)) || (config.enabled
                    || ConfigProvider.getConfig().getOptionalValue("camel.main.debugging", boolean.class).orElse(false));
        }
    }
}
