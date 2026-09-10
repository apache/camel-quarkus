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
import java.util.function.Predicate;
import java.util.regex.Pattern;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.CamelContext;
import org.apache.camel.console.DevConsole;
import org.apache.camel.console.DevConsoleRegistry;
import org.apache.camel.util.json.Jsoner;
import org.jboss.logging.Logger;

@Recorder
public class CamelDiagramRecorder {

    public Consumer<Route> route() {
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                // The handler negotiates and answers 406 itself. Restricting the route here makes a request for
                // another type fall through to a 404 instead
            }
        };
    }

    public Handler<RoutingContext> getHandler(RuntimeValue<CamelContext> contextRuntimeValue) {
        CamelContext context = contextRuntimeValue.getValue();
        DevConsoleRegistry devConsoleRegistry = context.getCamelContextExtension().getContextPlugin(DevConsoleRegistry.class);
        return new CamelDiagramHandler(devConsoleRegistry);
    }

    static final class CamelDiagramHandler implements Handler<RoutingContext> {
        private static final Logger LOG = Logger.getLogger(CamelDiagramHandler.class);

        private static final Predicate<String> BOOLEAN_VALUE = value -> "true".equals(value) || "false".equals(value);
        /** Route ids, endpoint URIs and source locations, optionally with wildcards. */
        private static final Predicate<String> FILTER_VALUE = Pattern.compile("[A-Za-z0-9_.,:/@*?% -]{1,100}")
                .asMatchPredicate();

        /**
         * The consoles the Dev UI diagram page reads, and the options its web components send to them. The id is
         * resolved against the registry, which holds every registered console, so without this an unrelated console
         * could be rendered through the diagram route. Options are copied from the query string straight into
         * {@link DevConsole#call(DevConsole.MediaType, Map)}, so anything absent from these rules is dropped, as
         * {@code CamelCoreDevUIService.sanitizeOptions} does for the Dev UI bridge.
         */
        private static final Map<String, Map<String, Predicate<String>>> DIAGRAM_CONSOLES = Map.of(
                "route-structure", Map.of(
                        "filter", FILTER_VALUE,
                        "metric", BOOLEAN_VALUE),
                "route-topology", Map.of(
                        "external", BOOLEAN_VALUE,
                        "metric", BOOLEAN_VALUE));

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
            if (id == null || !DIAGRAM_CONSOLES.containsKey(id)) {
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

            // The Dev UI web components render the diagrams from JSON, so JSON is all this route serves. Console text
            // output is markup for a browser to run, so anything else asked for, a top level browser navigation
            // included, is not acceptable here.
            String accept = ctx.request().getHeader("Accept");
            boolean wantsJson = accept != null && accept.contains("application/json");
            if (!wantsJson || !console.supportMediaType(DevConsole.MediaType.JSON)) {
                ctx.response().setStatusCode(406).end();
                return;
            }

            Object result = console.call(DevConsole.MediaType.JSON, sanitizeOptions(id, ctx.queryParams()));
            if (result != null) {
                ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .putHeader("X-Content-Type-Options", "nosniff")
                        .end(Jsoner.serialize(result));
            } else {
                ctx.response().setStatusCode(204).end();
            }
        }

        /**
         * Copies the query parameters allowed for the given console into the options passed to
         * {@link DevConsole#call(DevConsole.MediaType, Map)}. Unknown keys and values failing their rule are dropped.
         */
        private static Map<String, Object> sanitizeOptions(String id, MultiMap queryParams) {
            Map<String, Predicate<String>> rules = DIAGRAM_CONSOLES.get(id);
            Map<String, Object> options = new HashMap<>();
            queryParams.forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                Predicate<String> rule = rules.get(key);
                if (rule == null) {
                    LOG.debugf("Stripped disallowed option key '%s' for console '%s'", key, id);
                } else if (value == null || !rule.test(value)) {
                    LOG.debugf("Stripped option '%s' for console '%s' — value '%s' is not allowed", key, id, value);
                } else {
                    options.put(key, value);
                }
            });
            return options;
        }
    }
}
