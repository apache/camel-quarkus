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

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import io.vertx.ext.web.Router;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.component.platform.http.vertx.VertxPlatformHttpEngine;
import org.apache.camel.component.platform.http.vertx.VertxPlatformHttpRouter;
import org.apache.camel.quarkus.component.platform.http.runtime.PlatformHttpRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilterBuildItem;

class PlatformHttpProcessor {
    private static final String FEATURE = "camel-platform-http";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /*
     * The platform-http component is programmatically configured by the extension thus
     * we can safely prevent camel-quarkus-core to instantiate a default instance.
     */
    @BuildStep
    CamelServiceFilterBuildItem serviceFilter() {
        return new CamelServiceFilterBuildItem(CamelServiceFilter.forComponent("platform-http"));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    PlatformHttpEngineBuildItem platformHttpEngine(PlatformHttpRecorder recorder) {
        return new PlatformHttpEngineBuildItem(recorder.createEngine());
    }

    @BuildStep
    CamelRuntimeBeanBuildItem platformHttpEngineBean(PlatformHttpEngineBuildItem engine) {
        return new CamelRuntimeBeanBuildItem(
                PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME,
                VertxPlatformHttpEngine.class.getName(),
                engine.getInstance());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    CamelRuntimeBeanBuildItem platformHttpRouterBean(
            VertxBuildItem vertx,
            VertxWebRouterBuildItem router,
            BodyHandlerBuildItem bodyHandler,
            PlatformHttpRecorder recorder) {
        return new CamelRuntimeBeanBuildItem(
                VertxPlatformHttpRouter.PLATFORM_HTTP_ROUTER_NAME,
                Router.class.getName(),
                recorder.createVertxPlatformHttpRouter(vertx.getVertx(), router.getHttpRouter(), bodyHandler.getHandler()));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    CamelRuntimeBeanBuildItem platformHttpComponentBean(PlatformHttpRecorder recorder, PlatformHttpEngineBuildItem engine) {
        return new CamelRuntimeBeanBuildItem(
                PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME,
                PlatformHttpComponent.class.getName(),
                recorder.createComponent(engine.getInstance()));
    }
}
