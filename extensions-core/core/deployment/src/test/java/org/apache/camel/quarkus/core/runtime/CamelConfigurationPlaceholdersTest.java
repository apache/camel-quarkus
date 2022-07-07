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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CamelConfigurationPlaceholdersTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    private static long TIMEOUT = 1000;

    @Inject
    CamelContext context;

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.banner.enabled", "false");
        props.setProperty("direct.timeout", String.valueOf(TIMEOUT));

        // To verify profile activated properties that cannot be resolved do not break PropertiesComponent loading & resolution
        props.setProperty("%prod.non.resolvable.property", "${SOME_VALUE}");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @Test
    public void testPropertyPlaceholder() {
        DirectEndpoint endpoint = (DirectEndpoint) context.getRoute("directRoute").getConsumer().getEndpoint();
        Assertions.assertEquals(TIMEOUT, endpoint.getTimeout());
    }

    @ApplicationScoped
    public static class MyRoutes extends RouteBuilder {

        @Override
        public void configure() throws Exception {
            from("direct:start?timeout={{direct.timeout}}").routeId("directRoute")
                    .log("Hello World");
        }
    }
}
