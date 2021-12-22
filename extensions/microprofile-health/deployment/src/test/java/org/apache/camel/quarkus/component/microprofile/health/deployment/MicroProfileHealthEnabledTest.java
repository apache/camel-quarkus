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

import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.impl.health.ConsumersHealthCheckRepository;
import org.apache.camel.impl.health.ContextHealthCheck;
import org.apache.camel.impl.health.RoutesHealthCheckRepository;
import org.apache.camel.microprofile.health.CamelMicroProfileHealthCheckRegistry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MicroProfileHealthEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    CamelContext context;

    @Test
    public void healthCheckRegistryNotNull() {
        HealthCheckRegistry registry = HealthCheckRegistry.get(context);
        assertNotNull(registry);
        assertTrue(registry instanceof CamelMicroProfileHealthCheckRegistry);
        assertEquals("camel-microprofile-health", registry.getId());
    }

    @Test
    public void contextHealthCheckNotNull() {
        ContextHealthCheck contextHealthCheck = context.getRegistry().lookupByNameAndType("context", ContextHealthCheck.class);
        assertNotNull(contextHealthCheck);
    }

    @Test
    public void routesHealthCheckNotNull() {
        RoutesHealthCheckRepository routesRepository = context.getRegistry().lookupByNameAndType("routes",
                RoutesHealthCheckRepository.class);
        assertNotNull(routesRepository);
    }

    @Test
    public void consumersHealthCheckNotNull() {
        ConsumersHealthCheckRepository consumersRepository = context.getRegistry().lookupByNameAndType("consumers",
                ConsumersHealthCheckRepository.class);
        assertNotNull(consumersRepository);
    }
}
