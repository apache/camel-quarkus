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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.event.RouteStartedEvent;
import org.apache.camel.quarkus.core.CamelLifecycleEventBridge;
import org.apache.camel.spi.CamelEvent.RouteAddedEvent;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CamelManagementEventBridgeTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    EventHandler handler;

    @Inject
    CamelContext context;

    @Test
    public void testObservers() {
        // We're only observing management events so the lifecycle strategy should not be configured
        assertFalse(context.getLifecycleStrategies()
                .stream()
                .anyMatch(lifecycleStrategy -> lifecycleStrategy.getClass().equals(CamelLifecycleEventBridge.class)));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(handler.routesAdded())
                    .contains(MyRoutes.ROUTE_ID);
            assertThat(handler.routesStarted())
                    .contains(MyRoutes.ROUTE_ID);
        });
    }

    @ApplicationScoped
    public static class EventHandler {
        private final Set<String> routesAdded = new CopyOnWriteArraySet<>();
        private final Set<String> routesStarted = new CopyOnWriteArraySet<>();

        public void onRouteAdded(@Observes RouteAddedEvent event) {
            routesAdded.add(event.getRoute().getRouteId());
        }

        public void onRouteStarted(@Observes RouteStartedEvent event) {
            routesStarted.add(event.getRoute().getRouteId());
        }

        public Set<String> routesAdded() {
            return routesAdded;
        }

        public Set<String> routesStarted() {
            return routesStarted;
        }
    }

    @ApplicationScoped
    public static class MyRoutes extends RouteBuilder {
        public static String ROUTE_ID = "myRoute";
        public static String FROM_ENDPOINT = "direct://start";

        @Override
        public void configure() throws Exception {
            from(FROM_ENDPOINT)
                    .routeId(ROUTE_ID)
                    .log("${body}");
        }
    }
}
