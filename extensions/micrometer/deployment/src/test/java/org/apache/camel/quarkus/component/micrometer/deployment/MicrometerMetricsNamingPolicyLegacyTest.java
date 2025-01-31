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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifier;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifierNamingStrategyLegacy;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifier;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifierNamingStrategy;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryFactory;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryNamingStrategy;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy;
import org.apache.camel.component.micrometer.spi.InstrumentedThreadPoolFactory;
import org.apache.camel.spi.EventNotifier;
import org.apache.camel.spi.MessageHistoryFactory;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.spi.ThreadPoolFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MicrometerMetricsNamingPolicyLegacyTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @Inject
    CamelContext context;

    @Test
    void legacyNamingPolicy() {
        List<RoutePolicyFactory> routePolicyFactories = context.getRoutePolicyFactories();
        assertEquals(1, routePolicyFactories.size());
        RoutePolicyFactory routePolicyFactory = routePolicyFactories.get(0);
        assertInstanceOf(MicrometerRoutePolicyFactory.class, routePolicyFactory);
        MicrometerRoutePolicyFactory micrometerRoutePolicyFactory = (MicrometerRoutePolicyFactory) routePolicyFactory;
        assertEquals(MicrometerRoutePolicyNamingStrategy.LEGACY, micrometerRoutePolicyFactory.getNamingStrategy());

        MessageHistoryFactory messageHistoryFactory = context.getMessageHistoryFactory();
        assertNotNull(messageHistoryFactory);
        assertInstanceOf(MicrometerMessageHistoryFactory.class, messageHistoryFactory);

        MicrometerMessageHistoryFactory micrometerMessageHistoryFactory = (MicrometerMessageHistoryFactory) messageHistoryFactory;
        assertEquals(MicrometerMessageHistoryNamingStrategy.LEGACY, micrometerMessageHistoryFactory.getNamingStrategy());

        List<EventNotifier> eventNotifiers = context.getManagementStrategy()
                .getEventNotifiers()
                .stream()
                .filter(eventNotifier -> !eventNotifier.getClass().getName().contains("BaseMainSupport"))
                .toList();
        assertEquals(3, eventNotifiers.size());

        Optional<EventNotifier> optionalExchangeEventNotifier = context.getManagementStrategy()
                .getEventNotifiers()
                .stream()
                .filter(eventNotifier -> eventNotifier.getClass().equals(MicrometerExchangeEventNotifier.class))
                .findFirst();
        assertTrue(optionalExchangeEventNotifier.isPresent());

        MicrometerExchangeEventNotifier micrometerExchangeEventNotifier = (MicrometerExchangeEventNotifier) optionalExchangeEventNotifier
                .get();
        assertInstanceOf(MicrometerExchangeEventNotifierNamingStrategyLegacy.class,
                micrometerExchangeEventNotifier.getNamingStrategy());

        Optional<EventNotifier> optionalRouteEventNotifier = context.getManagementStrategy()
                .getEventNotifiers()
                .stream()
                .filter(eventNotifier -> eventNotifier.getClass().equals(MicrometerRouteEventNotifier.class))
                .findFirst();
        assertTrue(optionalRouteEventNotifier.isPresent());

        MicrometerRouteEventNotifier micrometerRouteEventNotifier = (MicrometerRouteEventNotifier) optionalRouteEventNotifier
                .get();
        assertEquals(MicrometerRouteEventNotifierNamingStrategy.LEGACY, micrometerRouteEventNotifier.getNamingStrategy());

        ThreadPoolFactory threadPoolFactory = context.getExecutorServiceManager().getThreadPoolFactory();
        assertNotNull(threadPoolFactory);
        assertFalse(threadPoolFactory instanceof InstrumentedThreadPoolFactory);
    }

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.camel.metrics.naming-strategy", "legacy");
        props.setProperty("quarkus.camel.metrics.enable-message-history", "true");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }
}
