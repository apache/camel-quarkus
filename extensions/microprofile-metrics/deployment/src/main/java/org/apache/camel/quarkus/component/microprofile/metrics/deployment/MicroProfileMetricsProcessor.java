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
package org.apache.camel.quarkus.component.microprofile.metrics.deployment;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.camel.component.microprofile.metrics.MicroProfileMetricsConstants;
import org.apache.camel.quarkus.component.microprofile.metrics.runtime.CamelMicroProfileMetricsConfig;
import org.apache.camel.quarkus.component.microprofile.metrics.runtime.CamelMicroProfileMetricsRecorder;
import org.apache.camel.quarkus.core.deployment.CamelBeanBuildItem;
import org.eclipse.microprofile.metrics.MetricRegistry;

class MicroProfileMetricsProcessor {

    private static final String FEATURE = "camel-microprofile-metrics";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem metricRegistry(CamelMicroProfileMetricsRecorder recorder) {
        return new CamelBeanBuildItem(
            MicroProfileMetricsConstants.METRIC_REGISTRY_NAME,
            MetricRegistry.class,
            recorder.createApplicationRegistry()
        );
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void configureCamelContext(CamelMicroProfileMetricsRecorder recorder, CamelMicroProfileMetricsConfig config, BeanContainerBuildItem beanContainer) {
        recorder.configureCamelContext(config, beanContainer.getValue());
    }
}
