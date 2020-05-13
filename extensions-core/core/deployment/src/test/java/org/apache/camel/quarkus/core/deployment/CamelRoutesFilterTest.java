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
package org.apache.camel.quarkus.core.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.lw.LightweightCamelContext;
import org.apache.camel.main.BaseMainSupport;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CamelRoutesFilterTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @Inject
    CamelContext camelContext;
    @Inject
    BaseMainSupport mainSupport;

    public static final Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.camel.main.routes-discovery.enabled", "true");
        props.setProperty("quarkus.camel.main.routes-discovery.exclude-patterns", "**/*Filtered");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @Test
    public void testRoutesFilter() {
        assertThat(camelContext.getRoutes()).hasSize(1);
        assertThat(camelContext.getRoutes()).first().hasFieldOrPropertyWithValue("id", "my-route");
        if (camelContext instanceof LightweightCamelContext) {
            assertNull(mainSupport.getRoutesBuilders());
        } else {
            assertThat(mainSupport.getRoutesBuilders()).hasSize(1);
            assertThat(mainSupport.getRoutesBuilders()).first().isInstanceOf(MyRoute.class);
        }
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
}
