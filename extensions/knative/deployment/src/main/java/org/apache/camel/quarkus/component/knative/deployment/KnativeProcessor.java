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
package org.apache.camel.quarkus.component.knative.deployment;

import java.util.List;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import org.apache.camel.component.knative.KnativeComponent;
import org.apache.camel.component.knative.KnativeConstants;
import org.apache.camel.component.knative.spi.KnativeEnvironment;
import org.apache.camel.component.knative.spi.KnativeResource;
import org.apache.camel.quarkus.component.knative.KnativeConsumerRecorder;
import org.apache.camel.quarkus.component.knative.KnativeProducerRecorder;
import org.apache.camel.quarkus.component.knative.KnativeRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilterBuildItem;
import org.apache.camel.spi.ComponentCustomizer;

class KnativeProcessor {

    private static final String FEATURE = "camel-knative";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<UnremovableBeanBuildItem> unremovableBeans() {
        return List.of(
                UnremovableBeanBuildItem.beanTypes(KnativeEnvironment.class));
    }

    @BuildStep
    List<ReflectiveClassBuildItem> reflectiveClasses() {
        return List.of(
                new ReflectiveClassBuildItem(true, false, KnativeEnvironment.class),
                new ReflectiveClassBuildItem(true, false, KnativeResource.class));
    }

    @BuildStep
    List<CamelServiceFilterBuildItem> servicesFilters() {
        return List.of(
                new CamelServiceFilterBuildItem(CamelServiceFilter.forComponent(KnativeConstants.SCHEME)),
                new CamelServiceFilterBuildItem(CamelServiceFilter
                        .forPathEndingWith(CamelServiceFilter.CAMEL_SERVICE_BASE_PATH + "/knative/transport/http")));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    CamelRuntimeBeanBuildItem knativeComponent(KnativeRecorder recorder) {
        return new CamelRuntimeBeanBuildItem(
                KnativeConstants.SCHEME,
                KnativeComponent.class.getName(),
                recorder.createKnativeComponent());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    CamelRuntimeBeanBuildItem knativeConsumerCustomizer(
            KnativeConsumerRecorder recorder,
            VertxWebRouterBuildItem router) {

        return new CamelRuntimeBeanBuildItem(
                FEATURE + "-consumer-customizer",
                ComponentCustomizer.class.getName(),
                recorder.createKnativeConsumerFactoryCustomizer(router.getHttpRouter()));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    CamelRuntimeBeanBuildItem knativeProducerCustomizer(
            KnativeProducerRecorder recorder,
            CoreVertxBuildItem vertx) {

        return new CamelRuntimeBeanBuildItem(
                FEATURE + "-producer-customizer",
                ComponentCustomizer.class.getName(),
                recorder.createKnativeProducerFactoryCustomizer(vertx.getVertx()));
    }
}
