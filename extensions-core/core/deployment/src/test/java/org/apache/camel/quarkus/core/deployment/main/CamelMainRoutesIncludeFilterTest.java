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
import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CamelMainRoutesIncludeFilterTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @Inject
    CamelContext context;

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.banner.enabled", "false");
        props.setProperty("quarkus.camel.routes-discovery.enabled", "true");
        props.setProperty("quarkus.camel.routes-discovery.include-patterns", "**/*Filtered");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @Test
    public void testRoutesFilter() {
        List<Route> routes = context.getRoutes();
        assertEquals(3, routes.size());
        assertNotNull(context.getRoute("my-route-filtered"));
        assertNotNull(context.getRoute("my-route-filteredBean"));
        assertNotNull(context.getRoute("my-route-filteredProduced"));
    }

    public static class MyRoute extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("direct:in").routeId("my-route").to("log:out");
        }
    }

    public static class MyRouteFiltered extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("direct:filtered").routeId("my-route-filtered").to("log:filtered");
        }
    }

    @ApplicationScoped
    public static class MyRouteBeanFiltered extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("direct:filteredBean").routeId("my-route-filteredBean").to("log:filteredBean");
        }
    }

    @ApplicationScoped
    public static class RouteBuilderProducerFiltered {
        @Produces
        @ApplicationScoped
        public RouteBuilder producedRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:produced").routeId("my-route-filteredProduced").to("log:produced");
                }
            };
        }
    }
}
