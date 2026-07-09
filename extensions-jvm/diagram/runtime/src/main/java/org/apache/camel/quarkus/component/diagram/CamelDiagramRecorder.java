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
package org.apache.camel.quarkus.component.diagram;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.CamelContext;
import org.apache.camel.console.DevConsole;
import org.apache.camel.console.DevConsoleRegistry;
import org.apache.camel.util.json.Jsoner;

@Recorder
public class CamelDiagramRecorder {

    public Consumer<Route> route() {
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                // No content type restriction — handler negotiates based on Accept header
            }
        };
    }

    public Handler<RoutingContext> getHandler(RuntimeValue<CamelContext> contextRuntimeValue) {
        CamelContext context = contextRuntimeValue.getValue();
        DevConsoleRegistry devConsoleRegistry = context.getCamelContextExtension().getContextPlugin(DevConsoleRegistry.class);
        return new CamelDiagramHandler(devConsoleRegistry);
    }

    static final class CamelDiagramHandler implements Handler<RoutingContext> {
        private final DevConsoleRegistry devConsoleRegistry;

        CamelDiagramHandler(DevConsoleRegistry devConsoleRegistry) {
            this.devConsoleRegistry = devConsoleRegistry;
        }

        @Override
        public void handle(RoutingContext ctx) {
            if (devConsoleRegistry == null || !devConsoleRegistry.isEnabled()) {
                ctx.response().setStatusCode(503).end();
                return;
            }

            String id = ctx.pathParam("id");
            if (id == null || id.isEmpty()) {
                ctx.response().setStatusCode(404).end();
                return;
            }

            DevConsole console = devConsoleRegistry.stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (console == null) {
                ctx.response().setStatusCode(404).end();
                return;
            }

            Map<String, Object> options = new HashMap<>();
            ctx.queryParams().forEach(entry -> options.put(entry.getKey(), entry.getValue()));

            String accept = ctx.request().getHeader("Accept");
            boolean wantsJson = accept != null && accept.contains("application/json");
            String format = ctx.queryParams().get("format");
            boolean wantsHtml = "html".equals(format);

            if (wantsJson && console.supportMediaType(DevConsole.MediaType.JSON)) {
                Object result = console.call(DevConsole.MediaType.JSON, options);
                if (result != null) {
                    ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end(Jsoner.serialize(result));
                } else {
                    ctx.response().setStatusCode(204).end();
                }
            } else if (console.supportMediaType(DevConsole.MediaType.TEXT)) {
                Object result = console.call(DevConsole.MediaType.TEXT, options);
                if (result != null) {
                    String contentType = wantsHtml ? "text/html" : "text/plain";
                    ctx.response()
                            .putHeader("Content-Type", contentType)
                            .end(result.toString());
                } else {
                    ctx.response().setStatusCode(204).end();
                }
            } else {
                ctx.response().setStatusCode(406).end();
            }
        }
    }
}
