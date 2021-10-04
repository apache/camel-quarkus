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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectComponent;
import org.apache.camel.quarkus.core.events.ComponentAddEvent;
import org.apache.camel.quarkus.core.events.EndpointAddEvent;
import org.apache.camel.quarkus.core.events.ServiceAddEvent;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CamelLifecycleEventBridgeTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    EventHandler handler;

    @Inject
    CamelContext context;

    @Test
    public void testObservers() {
        // We're only observing lifecycle events so the management strategy should not be configured
        assertTrue(context.getManagementStrategy().getEventNotifiers().isEmpty());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(handler.components())
                    .contains(DirectComponent.class.getName());
            assertThat(handler.endpoints())
                    .contains(MyRoutes.FROM_ENDPOINT);
            assertThat(handler.service())
                    .isNotEmpty();
        });
    }

    @ApplicationScoped
    public static class EventHandler {
        private final Set<String> endpoints = new CopyOnWriteArraySet<>();
        private final Set<String> components = new CopyOnWriteArraySet<>();
        private final Set<String> service = new CopyOnWriteArraySet<>();

        public void onComponentAdd(@Observes ComponentAddEvent event) {
            components.add(event.getComponent().getClass().getName());
        }

        public void onEndpointAdd(@Observes EndpointAddEvent event) {
            endpoints.add(event.getEndpoint().getEndpointUri());
        }

        public void onServiceAdd(@Observes ServiceAddEvent event) {
            service.add(event.getService().getClass().getName());
        }

        public Set<String> components() {
            return components;
        }

        public Set<String> endpoints() {
            return endpoints;
        }

        public Set<String> service() {
            return service;
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
