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
package org.apache.camel.quarkus.component.vertx.websocket;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.apache.camel.Endpoint;
import org.apache.camel.component.vertx.websocket.VertxWebsocketComponent;
import org.apache.camel.component.vertx.websocket.VertxWebsocketEndpoint;
import org.apache.camel.component.vertx.websocket.VertxWebsocketHost;
import org.apache.camel.component.vertx.websocket.VertxWebsocketHostConfiguration;
import org.apache.camel.component.vertx.websocket.VertxWebsocketHostKey;

@Recorder
public class VertxWebsocketRecorder {

    public RuntimeValue<VertxWebsocketComponent> createVertxWebsocketComponent(RuntimeValue<Vertx> vertx,
            RuntimeValue<Router> router) {
        QuarkusVertxWebsocketComponent component = new QuarkusVertxWebsocketComponent(router.getValue());
        component.setVertx(vertx.getValue());
        return new RuntimeValue<>(component);
    }

    static final class QuarkusVertxWebsocketComponent extends VertxWebsocketComponent {
        private final Router router;

        public QuarkusVertxWebsocketComponent(Router router) {
            this.router = router;
        }

        @Override
        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
            VertxWebsocketEndpoint endpoint = (VertxWebsocketEndpoint) super.createEndpoint(uri, remaining, parameters);
            endpoint.getConfiguration().setRouter(router);
            return endpoint;
        }

        @Override
        protected VertxWebsocketHost createVertxWebsocketHost(VertxWebsocketHostConfiguration hostConfiguration,
                VertxWebsocketHostKey hostKey) {
            return new QuarkusVertxWebsocketHost(hostConfiguration, hostKey);
        }
    }

    static final class QuarkusVertxWebsocketHost extends VertxWebsocketHost {
        public QuarkusVertxWebsocketHost(VertxWebsocketHostConfiguration websocketHostConfiguration,
                VertxWebsocketHostKey key) {
            super(websocketHostConfiguration, key);
        }

        @Override
        public void start() throws InterruptedException, ExecutionException {
            // Noop as quarkus-vertx-web handles the server lifecycle
        }
    }
}
