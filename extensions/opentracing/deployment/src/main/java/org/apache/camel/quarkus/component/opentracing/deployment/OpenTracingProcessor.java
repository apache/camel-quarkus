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
package org.apache.camel.quarkus.component.opentracing.deployment;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.jaeger.deployment.JaegerEnabled;
import org.apache.camel.opentracing.OpenTracingTracer;
import org.apache.camel.quarkus.component.opentracing.CamelOpenTracingConfig;
import org.apache.camel.quarkus.component.opentracing.CamelOpenTracingRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;

class OpenTracingProcessor {

    private static final String FEATURE = "camel-opentracing";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = JaegerEnabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    CamelBeanBuildItem setupCamelOpenTracingTracer(CamelOpenTracingConfig config, CamelOpenTracingRecorder recorder,
            BeanContainerBuildItem beanContainer) {
        // Configure & bind OpenTracingTracer to the registry so that Camel can use it
        return new CamelBeanBuildItem(
                "openTracingTracer",
                OpenTracingTracer.class.getName(),
                recorder.createCamelOpenTracingTracer(config, beanContainer.getValue()));
    }
}
