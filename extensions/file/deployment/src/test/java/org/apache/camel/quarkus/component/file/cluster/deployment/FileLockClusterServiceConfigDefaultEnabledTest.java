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
package org.apache.camel.quarkus.component.file.cluster.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.component.file.cluster.FileLockClusterService;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileLockClusterServiceConfigDefaultEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addAsResource(applicationProperties(),
                    "application.properties"));

    public static final Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.camel.cluster.file.enabled", "true");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @Inject
    CamelContext camelContext;

    @Test
    public void defaultEnabledConfigShouldAutoConfigureWithDefaults() {

        DefaultCamelContext dcc = camelContext.adapt(DefaultCamelContext.class);
        assertNotNull(dcc);

        FileLockClusterService[] flcs = dcc.getServices().stream().filter(s -> s instanceof FileLockClusterService)
                .toArray(FileLockClusterService[]::new);
        assertEquals(1, flcs.length);

        FileLockClusterService service = flcs[0];
        assertNotNull(service);

        assertNull(service.getId());
        assertNull(service.getRoot());
        assertEquals(Ordered.LOWEST, service.getOrder());
        assertNotNull(service.getAttributes());
        assertTrue(service.getAttributes().isEmpty());
        assertEquals(1L, service.getAcquireLockDelay());
        assertEquals(TimeUnit.SECONDS, service.getAcquireLockDelayUnit());
        assertEquals(10L, service.getAcquireLockInterval());
        assertEquals(TimeUnit.SECONDS, service.getAcquireLockIntervalUnit());
    }
}
