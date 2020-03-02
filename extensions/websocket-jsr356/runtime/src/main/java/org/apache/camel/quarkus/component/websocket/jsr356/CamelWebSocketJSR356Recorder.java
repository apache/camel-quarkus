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
package org.apache.camel.quarkus.component.websocket.jsr356;

import java.util.Collections;
import java.util.List;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.spec.ServletContextImpl;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.apache.camel.websocket.jsr356.CamelServerEndpoint;
import org.apache.camel.websocket.jsr356.JSR356WebSocketComponent;

@Recorder
public class CamelWebSocketJSR356Recorder {

    public void configureWebsocketEndpoints(WebSocketDeploymentInfo deploymentInfo, CamelWebSocketJSR356Config config) {
        List<String> endpointPaths = config.serverEndpointPaths.orElseGet(Collections::emptyList);
        for (String path : endpointPaths) {
            CamelServerEndpoint endpoint = new CamelServerEndpoint();
            ServerEndpointConfig.Builder builder = ServerEndpointConfig.Builder.create(CamelServerEndpoint.class, path);
            builder.configurator(new CamelWebSocketJSR356EndpointConfigurator(endpoint));
            deploymentInfo.addEndpoint(builder.build());
        }
    }

    public RuntimeValue<JSR356WebSocketComponent> createJsr356Component() {
        JSR356WebSocketComponent component = new JSR356WebSocketComponent();
        component.setServerEndpointDeploymentStrategy((container, configBuilder) -> {
            // Do nothing as the Quarkus Undertow extension handles deployment
        });
        return new RuntimeValue<>(component);
    }

    public void registerServerContainer(DeploymentManager deploymentManager) {
        ServletContextImpl servletContext = deploymentManager.getDeployment().getServletContext();
        ServerContainer container = (ServerContainer) servletContext.getAttribute(ServerContainer.class.getName());

        JSR356WebSocketComponent.registerServer(servletContext.getContextPath(), container);

        WebSocketDeploymentInfo deploymentInfo = (WebSocketDeploymentInfo) servletContext
                .getAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME);
        for (ServerEndpointConfig config : deploymentInfo.getProgramaticEndpoints()) {
            try {
                CamelServerEndpoint endpoint = config.getConfigurator().getEndpointInstance(CamelServerEndpoint.class);
                JSR356WebSocketComponent.ContextBag context = JSR356WebSocketComponent
                        .getContext(servletContext.getContextPath());
                context.getEndpoints().put(config.getPath(), endpoint);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
