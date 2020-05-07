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

import java.util.Collections;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.quarkus.component.platform.http.runtime.PlatformHttpRecorder;
import org.apache.camel.quarkus.component.platform.http.runtime.QuarkusPlatformHttpEngine;
import org.apache.camel.quarkus.core.deployment.spi.*;

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

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    PlatformHttpEngineBuildItem platformHttpEngine(
            PlatformHttpRecorder recorder) {

        return new PlatformHttpEngineBuildItem(recorder.createEngine());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    CamelServiceInitBuildItem initPlatformHttpEngine(
            PlatformHttpRecorder recorder,
            PlatformHttpEngineBuildItem engine,
            VertxWebRouterBuildItem router,
            BodyHandlerBuildItem bodyHandler,
            UploadAttacherBuildItem uploadAttacher) {

        recorder.initEngine(
                engine.getInstance(),
                router.getRouter(),
                Collections.singletonList(bodyHandler.getHandler()),
                uploadAttacher.getInstance());
        return new CamelServiceInitBuildItem("platform-http-engine");
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    PlatformHttpComponentBuildItem platformHttpComponent(
            PlatformHttpRecorder recorder,
            PlatformHttpEngineBuildItem engine) {
        return new PlatformHttpComponentBuildItem(recorder.createComponent(engine.getInstance()));
    }

    @BuildStep
    CamelRuntimeBeanBuildItem platformHttpEngineBean(PlatformHttpEngineBuildItem engine) {
        return new CamelRuntimeBeanBuildItem(
                PlatformHttpConstants.PLATFORM_HTTP_ENGINE_NAME,
                QuarkusPlatformHttpEngine.class.getName(),
                engine.getInstance());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem platformHttpComponentBean(PlatformHttpRecorder recorder, PlatformHttpComponentBuildItem component) {
        return new CamelBeanBuildItem(
                PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME,
                PlatformHttpComponent.class.getName(),
                component.getInstance());
    }

}
