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
package org.apache.camel.quarkus.component.infinispan.cluster.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.component.infinispan.remote.cluster.InfinispanRemoteClusterService;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfinispanClusterServiceConfigEnabledTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addAsResource(applicationProperties(),
                    "application.properties"));

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.infinispan-client.devservices.enabled", "false");
        props.setProperty("quarkus.infinispan-client.devservices.create-default-client", "false");
        props.setProperty("quarkus.camel.cluster.infinispan.id", "test-infinispan-cluster");
        props.setProperty("quarkus.camel.cluster.infinispan.order", "100");
        props.setProperty("quarkus.camel.cluster.infinispan.hosts", "localhost:11222");
        props.setProperty("quarkus.camel.cluster.infinispan.secure", "true");
        props.setProperty("quarkus.camel.cluster.infinispan.username", "testuser");
        props.setProperty("quarkus.camel.cluster.infinispan.password", "testpass");
        props.setProperty("quarkus.camel.cluster.infinispan.sasl-mechanism", "SCRAM-SHA-256");
        props.setProperty("quarkus.camel.cluster.infinispan.security-realm", "default");
        props.setProperty("quarkus.camel.cluster.infinispan.security-server-name", "infinispan");
        props.setProperty("quarkus.camel.cluster.infinispan.lifespan", "60");
        props.setProperty("quarkus.camel.cluster.infinispan.lifespan-time-unit", "SECONDS");
        props.setProperty("quarkus.camel.cluster.infinispan.attributes.attr1", "value1");
        props.setProperty("quarkus.camel.cluster.infinispan.attributes.attr2", "value2");
        props.setProperty("quarkus.camel.cluster.infinispan.configuration-properties.prop1", "propvalue1");
        props.setProperty("quarkus.camel.cluster.infinispan.configuration-properties.prop2", "propvalue2");

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
    void enabledConfigShouldAutoConfigure() {
        InfinispanRemoteClusterService[] icss = camelContext.getCamelContextExtension()
                .getServices()
                .stream()
                .filter(s -> s instanceof InfinispanRemoteClusterService)
                .toArray(InfinispanRemoteClusterService[]::new);
        assertEquals(1, icss.length);

        InfinispanRemoteClusterService ics = icss[0];
        assertNotNull(ics);

        assertEquals("test-infinispan-cluster", ics.getId());
        assertEquals(100, ics.getOrder());
        assertEquals("localhost:11222", ics.getHosts());
        assertTrue(ics.isSecure());
        assertEquals("testuser", ics.getUsername());
        assertEquals("testpass", ics.getPassword());
        assertEquals("SCRAM-SHA-256", ics.getSaslMechanism());
        assertEquals("default", ics.getSecurityRealm());
        assertEquals("infinispan", ics.getSecurityServerName());
        assertEquals(60L, ics.getLifespan());
        assertEquals(TimeUnit.SECONDS, ics.getLifespanTimeUnit());

        assertNotNull(ics.getAttributes());
        assertTrue(ics.getAttributes().containsKey("attr1"));
        assertEquals("value1", ics.getAttributes().get("attr1"));
        assertTrue(ics.getAttributes().containsKey("attr2"));
        assertEquals("value2", ics.getAttributes().get("attr2"));

        assertNotNull(ics.getConfigurationProperties());
        assertTrue(ics.getConfigurationProperties().containsKey("prop1"));
        assertEquals("propvalue1", ics.getConfigurationProperties().get("prop1"));
        assertTrue(ics.getConfigurationProperties().containsKey("prop2"));
        assertEquals("propvalue2", ics.getConfigurationProperties().get("prop2"));
    }

}
