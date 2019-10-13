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
package org.apache.camel.quarkus.component.platform.http.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import io.quarkus.vertx.web.deployment.BodyHandlerBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.quarkus.component.platform.http.runtime.PlatformHttpHandlers;
import org.apache.camel.quarkus.component.platform.http.runtime.PlatformHttpRecorder;
import org.apache.camel.quarkus.component.platform.http.runtime.QuarkusPlatformHttpEngine;
import org.apache.camel.quarkus.core.deployment.CamelRuntimeBeanBuildItem;

class PlatformHttpProcessor {

    private static final String FEATURE = "camel-platform-http";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    PlatformHttpEngineBuildItem platformHttpEngine(
            PlatformHttpRecorder recorder,
            VertxWebRouterBuildItem router,
            BodyHandlerBuildItem bodyHandler,
            List<FeatureBuildItem> features) {

        List<Handler<RoutingContext>> handlers = new ArrayList<>();

        //
        // When RESTEasy is added to the classpath, then the routes are paused
        // so we need to resume them.
        //
        //     https://github.com/quarkusio/quarkus/issues/4564
        //
        // TODO: remove this once the issue get fixed
        //
        if (features.stream().map(FeatureBuildItem::getInfo).anyMatch("resteasy"::equals)) {
            handlers.add(new PlatformHttpHandlers.Resumer());
        }

        handlers.add(bodyHandler.getHandler());

        return new PlatformHttpEngineBuildItem(
            recorder.createEngine(router.getRouter(), handlers)
        );
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
     CamelRuntimeBeanBuildItem platformHttpEngineBean(PlatformHttpRecorder recorder, PlatformHttpEngineBuildItem engine) {
        return new CamelRuntimeBeanBuildItem(
            PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME,
            QuarkusPlatformHttpEngine.class,
            engine.getInstance()
        );
    }


    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    CamelRuntimeBeanBuildItem platformHttpComponentBean(PlatformHttpRecorder recorder, PlatformHttpEngineBuildItem engine) {
        return new CamelRuntimeBeanBuildItem(
            PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME,
            PlatformHttpComponent.class,
            recorder.createComponent(engine.getInstance())
        );
    }
}
