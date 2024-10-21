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
package org.apache.camel.quarkus.component.opentelemetry.deployment;

import java.nio.file.Paths;
import java.util.Map;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.opentelemetry.deployment.tracing.TracerEnabled;
import org.apache.camel.quarkus.component.opentelemetry.OpenTelemetryTracerProducer;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.apache.camel.spi.FactoryFinder;

class OpenTelemetryProcessor {

    private static final String FEATURE = "camel-opentelemetry";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = TracerEnabled.class)
    AdditionalBeanBuildItem openTelemetryTracerProducerBean() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(OpenTelemetryTracerProducer.class)
                .build();
    }

    // TODO: Remove this: https://github.com/apache/camel-quarkus/issues/6669
    @BuildStep
    void overrideCamelOpenTelemetryThreadPoolServices(
            BuildProducer<CamelServicePatternBuildItem> camelServicePattern,
            BuildProducer<CamelServiceBuildItem> camelService) {

        Map.of("thread-pool-factory", "OpenTelemetryInstrumentedThreadPoolFactory",
                "thread-factory-listener", "OpenTelemetryInstrumentedThreadFactoryListener")
                .forEach((serviceName, type) -> {
                    String servicePath = FactoryFinder.DEFAULT_PATH + serviceName;
                    // Disable broken original service
                    camelServicePattern
                            .produce(new CamelServicePatternBuildItem(CamelServiceDestination.DISCOVERY, false, servicePath));

                    // Replace with working
                    camelService.produce(new CamelServiceBuildItem(Paths.get(servicePath),
                            "org.apache.camel.quarkus.component.opentelemetry.patch.%s".formatted(type)));
                });

    }
}
