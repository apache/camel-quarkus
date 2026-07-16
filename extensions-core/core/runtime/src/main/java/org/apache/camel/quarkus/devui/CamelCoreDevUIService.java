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

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.Cancellable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.CamelContext;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.catalog.RuntimeCamelCatalog;
import org.apache.camel.console.DevConsole;
import org.apache.camel.console.DevConsoleRegistry;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.json.DeserializationException;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
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

    /**
     * Gets the Camel catalog documentation of a component available in the application.
     *
     * @param  name          The component scheme
     * @param  optionsFilter Optional keyword to keep only the matching options
     * @return               JSON String representation of the component catalog documentation
     */
    @JsonRpcDescription("Camel catalog documentation of a component: endpoint URI syntax and all options"
            + " (name, type, default value, description), from the exact Camel version of this application."
            + " Use it before writing a route with an unfamiliar component instead of guessing URI options.")
    @DevMCPEnableByDefault
    public String camelCatalogDoc(
            @JsonRpcDescription("Component scheme, e.g. timer, kafka, sql") String name,
            @JsonRpcDescription("Optional keyword to keep only the options whose name or description matches") String optionsFilter) {
        RuntimeCamelCatalog catalog = getRuntimeCamelCatalog();
        if (catalog == null) {
            return "{}";
        }
        String json = catalog.componentJSonSchema(name);
        if (json == null) {
            JsonObject error = new JsonObject();
            error.put("error", "Component '" + name + "' is not on the application classpath. Add the camel-quarkus-"
                    + name + " dependency to the project.");
            return error.toJson();
        }
        if (optionsFilter == null || optionsFilter.isBlank()) {
            return json;
        }
        return filterComponentOptions(json, optionsFilter);
    }

    /**
     * Validates a Camel endpoint URI against the catalog of the application.
     *
     * @param  uri The full endpoint URI
     * @return     JSON String representation of the validation result
     */
    @JsonRpcDescription("Validates a Camel endpoint URI against the catalog of this application:"
            + " unknown options (with suggestions), invalid enum or boolean values, missing required options."
            + " Call it after writing or changing any endpoint URI.")
    @DevMCPEnableByDefault
    public String camelValidateEndpointUri(
            @JsonRpcDescription("Full endpoint URI, e.g. kafka:orders?brokers=localhost:9092") String uri) {
        RuntimeCamelCatalog catalog = getRuntimeCamelCatalog();
        if (catalog == null) {
            return "{}";
        }
        JsonObject result = new JsonObject();
        String scheme = uri != null ? StringHelper.before(uri, ":") : null;
        if (scheme == null || scheme.isBlank()) {
            result.put("valid", false);
            result.put("errors", "Malformed endpoint URI, expected scheme:path[?options]");
            return result.toJson();
        }
        if (catalog.componentJSonSchema(scheme) == null) {
            result.put("valid", false);
            result.put("errors", "Component '" + scheme + "' is not on the application classpath. Add the camel-quarkus-"
                    + scheme + " dependency to the project.");
            return result.toJson();
        }
        try {
            EndpointValidationResult validation = catalog.validateProperties(scheme, catalog.endpointProperties(uri));
            result.put("valid", validation.isSuccess());
            if (!validation.isSuccess()) {
                result.put("errors", validation.summaryErrorMessage(false));
            }
        } catch (URISyntaxException e) {
            result.put("valid", false);
            result.put("errors", "Malformed endpoint URI: " + e.getMessage());
        }
        return result.toJson();
    }

    private static String filterComponentOptions(String json, String filter) {
        try {
            JsonObject schema = (JsonObject) Jsoner.deserialize(json);
            String keyword = filter.toLowerCase(Locale.ROOT);
            for (String section : List.of("componentProperties", "properties", "headers")) {
                if (schema.get(section) instanceof JsonObject options) {
                    JsonObject filtered = new JsonObject();
                    for (Map.Entry<String, Object> option : options.entrySet()) {
                        if (optionMatches(option, keyword)) {
                            filtered.put(option.getKey(), option.getValue());
                        }
                    }
                    schema.put(section, filtered);
                }
            }
            return schema.toJson();
        } catch (DeserializationException e) {
            return json;
        }
    }

    private static boolean optionMatches(Map.Entry<String, Object> option, String keyword) {
        if (option.getKey().toLowerCase(Locale.ROOT).contains(keyword)) {
            return true;
        }
        if (option.getValue() instanceof Map<?, ?> attributes) {
            return attributes.get("description") instanceof String description
                    && description.toLowerCase(Locale.ROOT).contains(keyword);
        }
        return false;
    }

    private static RuntimeCamelCatalog getRuntimeCamelCatalog() {
        ArcContainer container = Arc.container();
        if (container == null) {
            // The app is likely performing a dev mode hot reload, thus the container may be temporarily unavailable
            return null;
        }

        return container
                .select(CamelContext.class)
                .get()
                .getCamelContextExtension()
                .getContextPlugin(RuntimeCamelCatalog.class);
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
