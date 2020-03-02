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
package org.apache.camel.quarkus.component.websocket.jsr356.deployment;

import java.util.List;

import javax.websocket.server.ServerEndpointConfig;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.undertow.deployment.ServletContextAttributeBuildItem;
import io.quarkus.undertow.deployment.ServletDeploymentManagerBuildItem;
import io.undertow.websockets.jsr.DefaultContainerConfigurator;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.apache.camel.quarkus.component.websocket.jsr356.CamelWebSocketJSR356Config;
import org.apache.camel.quarkus.component.websocket.jsr356.CamelWebSocketJSR356Recorder;
import org.apache.camel.quarkus.core.deployment.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.CamelServiceFilterBuildItem;
import org.apache.camel.websocket.jsr356.JSR356WebSocketComponent;

class WebSocketJSR356Processor {

    private static final String FEATURE = "camel-websocket-jsr356";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServiceProviderBuildItem registerConfiguratorServiceProvider() {
        // TODO: Remove this. See https://github.com/quarkusio/quarkus/issues/7509
        return new ServiceProviderBuildItem(ServerEndpointConfig.Configurator.class.getName(),
                DefaultContainerConfigurator.class.getName());
    }

    @BuildStep
    CamelServiceFilterBuildItem serviceFilter() {
        return new CamelServiceFilterBuildItem(CamelServiceFilter.forComponent("websocket-jsr356"));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    public void createWebsocketEndpoints(List<ServletContextAttributeBuildItem> servletContext,
            CamelWebSocketJSR356Recorder recorder, CamelWebSocketJSR356Config config) {
        ServletContextAttributeBuildItem wsDeploymentInfoAttribute = servletContext
                .stream()
                .filter(context -> context.getKey().equals(WebSocketDeploymentInfo.ATTRIBUTE_NAME))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Servlet context attribute: " + WebSocketDeploymentInfo.ATTRIBUTE_NAME + " not found"));

        recorder.configureWebsocketEndpoints((WebSocketDeploymentInfo) wsDeploymentInfoAttribute.getValue(), config);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    public void registerServerContainer(ServletDeploymentManagerBuildItem deploymentManager,
            CamelWebSocketJSR356Recorder recorder) {
        recorder.registerServerContainer(deploymentManager.getDeploymentManager());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    public CamelBeanBuildItem createWebSocketComponent(CamelWebSocketJSR356Recorder recorder) {
        return new CamelBeanBuildItem("websocket-jsr356", JSR356WebSocketComponent.class.getName(),
                recorder.createJsr356Component());
    }
}
