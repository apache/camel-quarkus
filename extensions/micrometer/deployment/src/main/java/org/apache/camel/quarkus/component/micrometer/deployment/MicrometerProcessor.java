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
package org.apache.camel.quarkus.component.micrometer.deployment;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.micrometer.deployment.MicrometerProcessor.MicrometerEnabled;
import io.quarkus.micrometer.deployment.RootMeterRegistryBuildItem;
import org.apache.camel.component.micrometer.MicrometerConstants;
import org.apache.camel.quarkus.component.micrometer.CamelMicrometerConfig;
import org.apache.camel.quarkus.component.micrometer.CamelMicrometerRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextCustomizerBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;

class MicrometerProcessor {

    private static final String FEATURE = "camel-micrometer";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = MicrometerEnabled.class)
    CamelBeanBuildItem meterRegistry(RootMeterRegistryBuildItem registry) {
        return new CamelBeanBuildItem(
                MicrometerConstants.METRICS_REGISTRY_NAME,
                MeterRegistry.class.getName(),
                registry.getValue());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = MicrometerEnabled.class)
    CamelContextCustomizerBuildItem contextCustomizer(
            CamelMicrometerRecorder recorder,
            CamelMicrometerConfig config) {

        return new CamelContextCustomizerBuildItem(recorder.createContextCustomizer(config));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = MicrometerEnabled.class)
    RuntimeCamelContextCustomizerBuildItem runtimeContextCustomizer(
            CamelMicrometerRecorder recorder,
            CamelMicrometerConfig config) {
        return new RuntimeCamelContextCustomizerBuildItem(recorder.createRuntimeContextCustomizer(config));
    }
}
