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
import org.apache.camel.Ordered;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KubernetesClusterServiceConfigEnabledWithoutRebalancingTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addAsResource(applicationProperties(),
                    "application.properties"));

    public static final Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.camel.cluster.kubernetes.enabled", "true");
        props.setProperty("quarkus.camel.cluster.kubernetes.rebalancing", "false");

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
    public void enabledConfigWithoutRebalancingAndDefaultsShouldAutoConfigure() {

        DefaultCamelContext dcc = camelContext.adapt(DefaultCamelContext.class);
        assertNotNull(dcc);

        KubernetesClusterService[] kcss = dcc.getServices().stream().filter(s -> s instanceof KubernetesClusterService)
                .toArray(KubernetesClusterService[]::new);
        assertEquals(1, kcss.length);

        KubernetesClusterService kcs = kcss[0];
        assertNotNull(kcs);

        assertNull(kcs.getId());
        assertNull(kcs.getMasterUrl());
        assertNull(kcs.getConnectionTimeoutMillis());
        assertNull(kcs.getKubernetesNamespace());
        assertNull(kcs.getPodName());
        assertEquals(1.2, kcs.getJitterFactor());
        assertEquals(15000L, kcs.getLeaseDurationMillis());
        assertEquals(10000L, kcs.getRenewDeadlineMillis());
        assertEquals(2000L, kcs.getRetryPeriodMillis());
        assertEquals(Ordered.LOWEST, kcs.getOrder());
        assertEquals("leaders", kcs.getKubernetesResourceName());
        assertEquals(LeaseResourceType.Lease, kcs.getLeaseResourceType());

        assertNotNull(kcs.getClusterLabels());
        assertTrue(kcs.getClusterLabels().isEmpty());
    }

}
