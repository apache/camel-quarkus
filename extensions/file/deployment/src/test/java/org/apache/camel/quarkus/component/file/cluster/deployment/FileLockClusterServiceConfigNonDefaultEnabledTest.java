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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileLockClusterServiceConfigNonDefaultEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addAsResource(applicationProperties(),
                    "application.properties"));

    public static final Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.camel.cluster.file.enabled", "true");
        props.setProperty("quarkus.camel.cluster.file.id", "service-id");
        props.setProperty("quarkus.camel.cluster.file.root", "root-path");
        props.setProperty("quarkus.camel.cluster.file.order", "10");
        props.setProperty("quarkus.camel.cluster.file.attributes.key1", "value1");
        props.setProperty("quarkus.camel.cluster.file.attributes.key2", "value2");
        props.setProperty("quarkus.camel.cluster.file.acquire-lock-delay", "5");
        props.setProperty("quarkus.camel.cluster.file.acquire-lock-interval", "1h");

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
    public void nonDefaultEnabledConfigShouldAutoConfigureWithoutDefaults() {

        DefaultCamelContext dcc = camelContext.adapt(DefaultCamelContext.class);
        assertNotNull(dcc);

        FileLockClusterService[] flcs = dcc.getServices().stream().filter(s -> s instanceof FileLockClusterService)
                .toArray(FileLockClusterService[]::new);
        assertEquals(1, flcs.length);

        FileLockClusterService service = flcs[0];
        assertNotNull(service);
        assertEquals("service-id", service.getId());
        assertEquals("root-path", service.getRoot());
        assertEquals(10, service.getOrder());

        assertNotNull(service.getAttributes());
        assertTrue(service.getAttributes().containsKey("key1"));
        assertEquals("value1", service.getAttributes().get("key1"));
        assertTrue(service.getAttributes().containsKey("key2"));
        assertEquals("value2", service.getAttributes().get("key2"));

        assertEquals(5L, service.getAcquireLockDelay());
        assertEquals(TimeUnit.MILLISECONDS, service.getAcquireLockDelayUnit());
        assertEquals(3600000, service.getAcquireLockInterval());
        assertEquals(TimeUnit.MILLISECONDS, service.getAcquireLockIntervalUnit());
    }
}
