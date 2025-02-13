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
package org.apache.camel.quarkus.component.console;

import java.util.Map;
import java.util.function.Consumer;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.console.DevConsole;
import org.apache.camel.console.DevConsoleRegistry;
import org.apache.camel.util.ObjectHelper;

@Recorder
public class CamelConsoleRecorder {
    public Consumer<Route> route() {
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                route.produces("application/json");
            }
        };
    }

    public Handler<RoutingContext> getHandler(RuntimeValue<CamelContext> contextRuntimeValue) {
        CamelContext context = contextRuntimeValue.getValue();
        DevConsoleRegistry devConsoleRegistry = context.getCamelContextExtension().getContextPlugin(DevConsoleRegistry.class);
        return new CamelConsoleHandler(devConsoleRegistry);
    }

    public void initDevConsoleRegistry(RuntimeValue<CamelContext> camelContextRuntimeValue) {
        camelContextRuntimeValue.getValue()
                .getCamelContextExtension()
                .getContextPlugin(DevConsoleRegistry.class)
                .loadDevConsoles();
    }

    static final class CamelConsoleHandler implements Handler<RoutingContext> {
        private final DevConsoleRegistry devConsoleRegistry;

        CamelConsoleHandler(DevConsoleRegistry devConsoleRegistry) {
            this.devConsoleRegistry = devConsoleRegistry;
        }

        @Override
        public void handle(RoutingContext context) {
            if (devConsoleRegistry != null && devConsoleRegistry.isEnabled()) {
                String id = context.pathParam("id");
                if (ObjectHelper.isNotEmpty(id)) {
                    getConsoleById(context, id);
                } else {
                    getConsoles(context);
                }
            }
        }

        void getConsoles(RoutingContext context) {
            JsonObject root = new JsonObject();
            devConsoleRegistry.stream().forEach(devConsole -> {
                JsonObject console = new JsonObject();
                console.put("id", devConsole.getId());
                console.put("displayName", devConsole.getDisplayName());
                console.put("description", devConsole.getDescription());
                root.put(devConsole.getId(), console);
            });
            writeResponse(context.response(), root);
        }

        void getConsoleById(RoutingContext context, String id) {
            JsonObject root = new JsonObject();
            devConsoleRegistry.stream().sorted((o1, o2) -> {
                int p1 = id.indexOf(o1.getId());
                int p2 = id.indexOf(o2.getId());
                return Integer.compare(p1, p2);
            }).forEach(devConsole -> {
                boolean include = "all".equals(id) || id.contains(devConsole.getId());
                if (include && devConsole.supportMediaType(DevConsole.MediaType.JSON)) {
                    Object result = devConsole.call(DevConsole.MediaType.JSON, Map.of(Exchange.HTTP_PATH, id));
                    if (result != null) {
                        root.put(devConsole.getId(), result);
                    }
                }
            });
            writeResponse(context.response(), root);
        }

        void writeResponse(HttpServerResponse response, JsonObject jsonObject) {
            response.putHeader("Content-Type", "application/json");
            response.end(jsonObject.encode());
        }
    }
}
