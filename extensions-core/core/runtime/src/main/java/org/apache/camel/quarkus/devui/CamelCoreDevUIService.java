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
package org.apache.camel.quarkus.devui;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.Cancellable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.CamelContext;
import org.apache.camel.console.DevConsole;
import org.apache.camel.console.DevConsoleRegistry;
import org.apache.camel.util.json.JsonObject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

/**
 * Dev UI service to return JSON representations of Camel DevConsole data in a streaming or non-streaming way.
 */
@ApplicationScoped
public class CamelCoreDevUIService {
    private static final Logger LOG = Logger.getLogger(CamelCoreDevUIService.class);
    private static volatile Set<String> ALLOWED_CONSOLE_IDS = Collections.emptySet();
    private static volatile Map<String, Map<String, Set<String>>> ALLOWED_CONSOLE_OPTIONS = Collections.emptyMap();
    private final Map<String, ConsoleSubscription> PROCESSORS = new ConcurrentHashMap<>();

    static void setAllowedConsoleIds(Set<String> consoleIds) {
        ALLOWED_CONSOLE_IDS = Set.copyOf(consoleIds);
    }

    static void setAllowedConsoleOptions(Map<String, String> optionSpecs) {
        Map<String, Map<String, Set<String>>> parsed = new HashMap<>();
        for (Map.Entry<String, String> entry : optionSpecs.entrySet()) {
            String spec = entry.getValue();
            Map<String, Set<String>> optionRules = new HashMap<>();
            if (spec != null && !spec.isEmpty()) {
                for (String optionEntry : spec.split(";")) {
                    String[] parts = optionEntry.split("=", 2);
                    if (parts.length == 2) {
                        String optionKey = parts[0].trim();
                        String valuesStr = parts[1].trim();
                        if ("*".equals(valuesStr)) {
                            optionRules.put(optionKey, Set.of("*"));
                        } else {
                            optionRules.put(optionKey, Set.of(valuesStr.split("\\|")));
                        }
                    }
                }
            }
            parsed.put(entry.getKey(), Collections.unmodifiableMap(optionRules));
        }
        ALLOWED_CONSOLE_OPTIONS = Collections.unmodifiableMap(parsed);
    }

    /**
     * Deactivates a Dev UI subscription for the given Camel console id.
     *
     * @param  id The id of the Camel console
     * @return    {@code true} if the console was deactivated, else {@code false}
     */
    public boolean deactivateConsoleStream(String id) {
        ConsoleSubscription subscription = PROCESSORS.remove(id);
        if (subscription != null) {
            synchronized (subscription) {
                subscription.cancel();
            }
            return true;
        }
        return false;
    }

    /**
     * Gets a JSON String representation of a Camel console.
     *
     * @param  id      The id of the Camel console
     * @param  options The map of options to influence the JSON output
     * @return         JSON String representation of the Camel console
     */
    public String getConsoleJSON(String id, Map<String, Object> options) {
        if (!isConsoleAllowed(id)) {
            LOG.debugf("Rejected Dev UI request for console '%s' — not in the allowlist", id);
            return "{}";
        }
        return getOrCreateConsoleSubscription(id, sanitizeOptions(id, options)).callDevConsole();
    }

    /**
     * Streams a JSON String representation of a Camel console.
     *
     * @param  id      The id of the Camel console
     * @param  options The map of options to influence the JSON output
     * @return         {@link Multi} of JSON String representation of the Camel console
     */
    public Multi<String> streamConsole(String id, Map<String, Object> options) {
        if (!isConsoleAllowed(id)) {
            LOG.debugf("Rejected Dev UI stream request for console '%s' — not in the allowlist", id);
            return Multi.createFrom().empty();
        }
        return getOrCreateConsoleSubscription(id, sanitizeOptions(id, options)).getBroadcaster();
    }

    /**
     * Updates the map of options for a given console subscription.
     *
     * @param  id      The id of the Camel console
     * @param  options The updated map of options to influence the JSON output
     * @return         {@code true} if the options were updated, else {@code false}
     */
    public boolean updateConsoleOptions(String id, Map<String, Object> options) {
        if (!isConsoleAllowed(id)) {
            LOG.debugf("Rejected Dev UI options update for console '%s' — not in the allowlist", id);
            return false;
        }
        Map<String, Object> sanitized = sanitizeOptions(id, options);
        return PROCESSORS.computeIfPresent(id, (key, consoleSubscription) -> {
            consoleSubscription.setOptions(sanitized);
            return consoleSubscription;
        }) != null;
    }

    private static boolean isConsoleAllowed(String id) {
        return ALLOWED_CONSOLE_IDS.contains(id);
    }

    private static Map<String, Object> sanitizeOptions(String id, Map<String, Object> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Set<String>> rules = ALLOWED_CONSOLE_OPTIONS.get(id);
        if (rules == null || rules.isEmpty()) {
            if (!options.isEmpty()) {
                LOG.debugf("Stripped all options for console '%s' — no options are allowed", id);
            }
            return Collections.emptyMap();
        }
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            String key = entry.getKey();
            Set<String> allowedValues = rules.get(key);
            if (allowedValues == null) {
                LOG.debugf("Stripped disallowed option key '%s' for console '%s'", key, id);
                continue;
            }
            String stringValue = String.valueOf(entry.getValue());
            if (allowedValues.contains("*") || allowedValues.contains(stringValue)) {
                sanitized.put(key, entry.getValue());
            } else {
                LOG.debugf("Stripped option '%s' for console '%s' — value '%s' is not allowed", key, id, stringValue);
            }
        }
        return sanitized;
    }

    ConsoleSubscription getOrCreateConsoleSubscription(String id, Map<String, Object> options) {
        return PROCESSORS.computeIfAbsent(id, consoleId -> new ConsoleSubscription(consoleId, options));
    }

    static DevConsoleRegistry getDevConsoleRegistry() {
        ArcContainer container = Arc.container();
        if (container == null) {
            // The app is likely performing a dev mode hot reload, thus the container may be temporarily unavailable
            return null;
        }

        return container
                .select(CamelContext.class)
                .get()
                .getCamelContextExtension()
                .getContextPlugin(DevConsoleRegistry.class);
    }

    /**
     * Enables any Camel console to have its data streamed and updated within a specified period.
     */
    static final class ConsoleSubscription {
        private final String id;
        private final Map<String, Object> options;
        private final Cancellable cancellable;
        private final BroadcastProcessor<String> broadcaster;

        ConsoleSubscription(String id, Map<String, Object> options) {
            this.id = id;
            this.options = new ConcurrentHashMap<>(options);
            this.broadcaster = BroadcastProcessor.create();
            this.cancellable = Multi.createFrom()
                    .ticks()
                    .every(ConfigProvider.getConfig().getValue("quarkus.camel.dev-ui.update-interval", Duration.class))
                    .subscribe()
                    .with(this::broadcast);
        }

        void broadcast(Long invocationCount) {
            broadcaster.onNext(callDevConsole());
        }

        void cancel() {
            cancellable.cancel();
        }

        BroadcastProcessor<String> getBroadcaster() {
            return broadcaster;
        }

        String callDevConsole() {
            String result = "{}";
            DevConsoleRegistry devConsoleRegistry = getDevConsoleRegistry();
            if (devConsoleRegistry != null) {
                Optional<DevConsole> devConsoleOptional = devConsoleRegistry.getConsole(id);
                if (devConsoleOptional.isPresent()) {
                    DevConsole devConsole = devConsoleOptional.get();
                    try {
                        JsonObject json = (JsonObject) devConsole.call(DevConsole.MediaType.JSON, options);
                        if (json != null) {
                            result = json.toJson();
                            LOG.debug(result);
                        }
                    } catch (Exception e) {
                        LOG.debugf(e, "Error calling DevConsole %s", id);
                    }
                }
            }
            return result;
        }

        void setOptions(Map<String, Object> options) {
            synchronized (this.options) {
                this.options.clear();
                this.options.putAll(options);
            }
        }
    }
}
