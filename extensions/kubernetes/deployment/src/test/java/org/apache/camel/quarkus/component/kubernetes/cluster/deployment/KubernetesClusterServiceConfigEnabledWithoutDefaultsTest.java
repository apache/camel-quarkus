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
package org.apache.camel.quarkus.component.kubernetes.cluster.deployment;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.component.kubernetes.cluster.KubernetesClusterService;
import org.apache.camel.component.kubernetes.cluster.LeaseResourceType;
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

public class KubernetesClusterServiceConfigEnabledWithoutDefaultsTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addAsResource(applicationProperties(),
                    "application.properties"));

    public static final Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.camel.cluster.kubernetes.enabled", "true");
        props.setProperty("quarkus.camel.cluster.kubernetes.id", "kcs-id");
        props.setProperty("quarkus.camel.cluster.kubernetes.master-url", "kcs-master-url");
        props.setProperty("quarkus.camel.cluster.kubernetes.connection-timeout-millis", "5033");
        props.setProperty("quarkus.camel.cluster.kubernetes.namespace", "kcs-namespace");
        props.setProperty("quarkus.camel.cluster.kubernetes.pod-name", "kcs-pod-name");
        props.setProperty("quarkus.camel.cluster.kubernetes.jitter-factor", "1.5034");
        props.setProperty("quarkus.camel.cluster.kubernetes.lease-duration-millis", "5036");
        props.setProperty("quarkus.camel.cluster.kubernetes.renew-deadline-millis", "5037");
        props.setProperty("quarkus.camel.cluster.kubernetes.retry-period-millis", "5038");
        props.setProperty("quarkus.camel.cluster.kubernetes.order", "5039");
        props.setProperty("quarkus.camel.cluster.kubernetes.resource-name", "kcs-resource-name");
        props.setProperty("quarkus.camel.cluster.kubernetes.lease-resource-type", "config-map");
        props.setProperty("quarkus.camel.cluster.kubernetes.rebalancing", "false");
        props.setProperty("quarkus.camel.cluster.kubernetes.labels.key1", "value1");
        props.setProperty("quarkus.camel.cluster.kubernetes.labels.key2", "value2");

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
    public void enabledConfigWithoutDefaultsShouldAutoConfigure() {

        DefaultCamelContext dcc = camelContext.adapt(DefaultCamelContext.class);
        assertNotNull(dcc);

        KubernetesClusterService[] kcss = dcc.getServices().stream().filter(s -> s instanceof KubernetesClusterService)
                .toArray(KubernetesClusterService[]::new);
        assertEquals(1, kcss.length);

        KubernetesClusterService kcs = kcss[0];
        assertNotNull(kcs);

        assertEquals("kcs-id", kcs.getId());
        assertEquals("kcs-master-url", kcs.getMasterUrl());
        assertEquals(5033, kcs.getConnectionTimeoutMillis());
        assertEquals("kcs-namespace", kcs.getKubernetesNamespace());
        assertEquals("kcs-pod-name", kcs.getPodName());
        assertEquals(1.5034, kcs.getJitterFactor());
        assertEquals(5036, kcs.getLeaseDurationMillis());
        assertEquals(5037, kcs.getRenewDeadlineMillis());
        assertEquals(5038, kcs.getRetryPeriodMillis());
        assertEquals(5039, kcs.getOrder());
        assertEquals("kcs-resource-name", kcs.getKubernetesResourceName());
        assertEquals(LeaseResourceType.ConfigMap, kcs.getLeaseResourceType());

        assertNotNull(kcs.getClusterLabels());
        assertTrue(kcs.getClusterLabels().containsKey("key1"));
        assertEquals("value1", kcs.getClusterLabels().get("key1"));
        assertTrue(kcs.getClusterLabels().containsKey("key2"));
        assertEquals("value2", kcs.getClusterLabels().get("key2"));
    }

}
