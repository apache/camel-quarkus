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
package org.apache.camel.quarkus.core.runtime;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.event.RouteAddedEvent;
import org.apache.camel.quarkus.core.CamelLifecycleEventBridge;
import org.apache.camel.quarkus.core.events.ServiceAddEvent;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.apache.camel.spi.CamelEvent.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CamelEventBridgeDisabledConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @Inject
    CamelContext context;

    @Inject
    EventHandler handler;

    @Test
    public void camelManagementEventBridgeNotConfigured() {
        assertFalse(context.getLifecycleStrategies()
                .stream()
                .anyMatch(lifecycleStrategy -> lifecycleStrategy.getClass().equals(CamelLifecycleEventBridge.class)));
        assertTrue(context.getManagementStrategy()
                .getEventNotifiers()
                .stream()
                .filter(eventNotifier -> !eventNotifier.getClass().getName().contains("BaseMainSupport"))
                .findAny()
                .isEmpty());
        assertTrue(handler.getServices().isEmpty());
        assertTrue(handler.getRoutesAdded().isEmpty());
        assertTrue(handler.getRoutesRemoved().isEmpty());
    }

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.banner.enabled", "false");
        props.setProperty("quarkus.camel.event-bridge.enabled", "false");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @ApplicationScoped
    static final class EventHandler {
        private final Set<ServiceAddEvent> services = new CopyOnWriteArraySet<>();
        private final Set<RouteAddedEvent> routesAdded = new CopyOnWriteArraySet<>();
        private final Set<RouteRemovedEvent> routesRemoved = new CopyOnWriteArraySet<>();

        public void onServiceAdd(@Observes ServiceAddEvent event) {
            services.add(event);
        }

        public void onRouteAdd(@Observes RouteAddedEvent event) {
            routesAdded.add(event);
        }

        public void onRouteRemoved(@Observes RouteRemovedEvent event) {
            routesRemoved.add(event);
        }

        public Set<ServiceAddEvent> getServices() {
            return services;
        }

        public Set<RouteAddedEvent> getRoutesAdded() {
            return routesAdded;
        }

        public Set<RouteRemovedEvent> getRoutesRemoved() {
            return routesRemoved;
        }
    }
}
