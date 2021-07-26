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
package org.apache.camel.quarkus.component.microprofile.metrics.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.component.microprofile.metrics.message.history.MicroProfileMetricsMessageHistoryFactory;
import org.apache.camel.quarkus.core.CamelManagementEventBridge;
import org.apache.camel.spi.EventNotifier;
import org.apache.camel.spi.MessageHistoryFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MicroProfileMetricsConfigOverrideTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @Inject
    CamelContext context;

    //@Test
    public void testMicroProfileMetricsConfiguration() {
        assertTrue(context.getRoutePolicyFactories().isEmpty());

        MessageHistoryFactory messageHistoryFactory = context.getMessageHistoryFactory();
        assertNotNull(messageHistoryFactory);
        assertTrue(messageHistoryFactory instanceof MicroProfileMetricsMessageHistoryFactory);

        // There should always be one event notifier added by core for CamelManagementEventBridge
        List<EventNotifier> eventNotifiers = context.getManagementStrategy().getEventNotifiers();
        assertEquals(1, eventNotifiers.size());
        assertTrue(eventNotifiers.get(0) instanceof CamelManagementEventBridge);
    }

    public static final Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.banner.enabled", "false");
        props.setProperty("quarkus.arc.remove-unused-beans", "false");
        props.setProperty("quarkus.camel.metrics.enable-route-policy", "false");
        props.setProperty("quarkus.camel.metrics.enable-message-history", "true");
        props.setProperty("quarkus.camel.metrics.enable-exchange-event-notifier", "false");
        props.setProperty("quarkus.camel.metrics.enable-route-event-notifier", "false");
        props.setProperty("quarkus.camel.metrics.enable-camel-context-event-notifier", "false");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }
}
