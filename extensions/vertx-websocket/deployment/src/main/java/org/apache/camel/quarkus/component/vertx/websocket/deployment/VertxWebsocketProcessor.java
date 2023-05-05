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
package org.apache.camel.quarkus.component.vertx.websocket.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import org.apache.camel.component.vertx.websocket.VertxWebsocketComponent;
import org.apache.camel.quarkus.component.vertx.websocket.VertxWebsocketRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;

class VertxWebsocketProcessor {

    private static final String FEATURE = "camel-vertx-websocket";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    CamelRuntimeBeanBuildItem configureVertxWebsocketComponent(
            VertxBuildItem vertx,
            VertxWebRouterBuildItem router,
            LaunchModeBuildItem launchMode,
            HttpConfiguration httpConfig,
            VertxWebsocketRecorder recorder) {
        return new CamelRuntimeBeanBuildItem("vertx-websocket", VertxWebsocketComponent.class.getName(),
                recorder.createVertxWebsocketComponent(vertx.getVertx(), router.getHttpRouter(), launchMode.getLaunchMode(),
                        httpConfig));
    }
}
