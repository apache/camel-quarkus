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
package org.apache.camel.quarkus.dsl.yaml.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.dsl.yaml.common.YamlDeserializationMode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YamlDslClassicModeTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("routes/routes.yaml", "routes/routes.yaml")
                    .addAsResource(applicationProperties(), "application.properties"));

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.put("camel.main.routes-include-pattern", "routes/routes.yaml");
        props.put("quarkus.camel.yaml.flow-mode", "false");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @Test
    public void classicModeEnabled() {
        String mode = context.getGlobalOptions().get(YamlRoutesBuilderLoader.DESERIALIZATION_MODE);
        assertEquals(YamlDeserializationMode.CLASSIC.name(), mode);
    }

    @Test
    public void classicModeYamlRoute() throws InterruptedException {
        MockEndpoint endpoint = context.getEndpoint("mock:split", MockEndpoint.class);
        endpoint.expectedBodiesReceived("foo", "bar", "cheese");

        producerTemplate.sendBody("direct:classicMode", "foo,bar,cheese");

        endpoint.assertIsSatisfied(5000);
    }
}
