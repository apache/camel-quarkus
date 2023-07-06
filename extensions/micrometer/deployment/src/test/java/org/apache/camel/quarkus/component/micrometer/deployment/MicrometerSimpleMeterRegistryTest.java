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
package org.apache.camel.quarkus.component.micrometer.deployment;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.component.micrometer.MicrometerComponent;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class MicrometerSimpleMeterRegistryTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));
    @Inject
    CamelContext context;

    @Test
    public void testSimpleRegistryRegistration() throws Exception {
        MicrometerComponent component = context.getComponent("micrometer", MicrometerComponent.class);
        assertInstanceOf(CompositeMeterRegistry.class, component.getMetricsRegistry(), "CompositeMeterRegistry");
        assertEquals(1, ((CompositeMeterRegistry) component.getMetricsRegistry()).getRegistries().size(), "! registry");
        assertInstanceOf(SimpleMeterRegistry.class,
                ((CompositeMeterRegistry) component.getMetricsRegistry()).getRegistries().iterator().next(),
                "SimpleMeterRegistry");
    }
}
