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
package org.apache.camel.quarkus.core.deployment.main;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.quarkus.main.CamelMain;
import org.apache.camel.quarkus.main.CamelMainEventBridge;
import org.apache.camel.quarkus.main.events.AfterConfigure;
import org.apache.camel.quarkus.main.events.AfterStart;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CamelMainEventBridgeDisabledConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @Inject
    CamelMain main;

    @Inject
    EventHandler handler;

    @Test
    public void testObservers() {
        assertFalse(main.getMainListeners().stream().anyMatch(mainListener -> mainListener
                .getClass()
                .equals(CamelMainEventBridge.class)));
        assertTrue(handler.builders().isEmpty());
        assertTrue(handler.routes().isEmpty());
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
    public static class EventHandler {
        private final Set<String> builders = new CopyOnWriteArraySet<>();
        private final Set<String> routes = new CopyOnWriteArraySet<>();

        public void afterConfigure(@Observes AfterConfigure event) {
            event.getMain().configure().getRoutesBuilders().forEach(
                    builder -> builders.add(builder.getClass().getName()));
        }

        public void afterStart(@Observes AfterStart event) {
            event.getCamelContext(ModelCamelContext.class).getRoutes().forEach(
                    route -> routes.add(route.getRouteId()));
        }

        public Set<String> builders() {
            return builders;
        }

        public Set<String> routes() {
            return routes;
        }
    }

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
