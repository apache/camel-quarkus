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
package org.apache.camel.quarkus.component.microprofile.health.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.impl.health.ConsumersHealthCheckRepository;
import org.apache.camel.impl.health.ContextHealthCheck;
import org.apache.camel.impl.health.RoutesHealthCheckRepository;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MicroProfileHealthCamelChecksDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @Inject
    CamelContext context;

    public static final Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.put("camel.health.contextEnabled", "false");
        props.put("camel.health.routesEnabled", "false");
        props.put("camel.health.consumersEnabled", "false");
        props.put("camel.health.registryEnabled", "false");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @Test
    public void contextHealthCheckNull() {
        ContextHealthCheck contextHealthCheck = context.getRegistry().lookupByNameAndType("context", ContextHealthCheck.class);
        assertNull(contextHealthCheck);
    }

    @Test
    public void routesHealthCheckNull() {
        RoutesHealthCheckRepository routesRepository = context.getRegistry().lookupByNameAndType("routes",
                RoutesHealthCheckRepository.class);
        assertNull(routesRepository);
    }

    @Test
    public void consumersHealthCheckNull() {
        ConsumersHealthCheckRepository consumersRepository = context.getRegistry().lookupByNameAndType("consumers",
                ConsumersHealthCheckRepository.class);
        assertNull(consumersRepository);
    }

    @Test
    public void healthRegistryNull() {
        Set<HealthCheckRegistry> healthCheckRegistries = context.getRegistry().findByType(HealthCheckRegistry.class);
        assertTrue(healthCheckRegistries.isEmpty());
    }
}
