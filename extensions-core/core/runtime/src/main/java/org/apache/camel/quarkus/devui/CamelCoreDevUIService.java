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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.arc.Arc;
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
    private final Map<String, ConsoleSubscription> PROCESSORS = new ConcurrentHashMap<>();

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
        return getOrCreateConsoleSubscription(id, options).callDevConsole();
    }

    /**
     * Streams a JSON String representation of a Camel console.
     *
     * @param  id      The id of the Camel console
     * @param  options The map of options to influence the JSON output
     * @return         {@link Multi} of JSON String representation of the Camel console
     */
    public Multi<String> streamConsole(String id, Map<String, Object> options) {
        return getOrCreateConsoleSubscription(id, options).getBroadcaster();
    }

    /**
     * Updates the map of options for a given console subscription.
     *
     * @param  id      The id of the Camel console
     * @param  options The updated map of options to influence the JSON output
     * @return         {@code true} if the options were updated, else {@code false}
     */
    public boolean updateConsoleOptions(String id, Map<String, Object> options) {
        return PROCESSORS.computeIfPresent(id, (key, consoleSubscription) -> {
            consoleSubscription.setOptions(options);
            return consoleSubscription;
        }) != null;
    }

    ConsoleSubscription getOrCreateConsoleSubscription(String id, Map<String, Object> options) {
        return PROCESSORS.computeIfAbsent(id, consoleId -> new ConsoleSubscription(consoleId, options));
    }

    static DevConsoleRegistry getDevConsoleRegistry() {
        return Arc.container()
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
