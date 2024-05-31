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
package org.apache.camel.quarkus.core.runtime;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.debugger.BacklogTracer;
import org.apache.camel.spi.Registry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelTraceTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @Inject
    Registry registry;

    @Inject
    CamelContext camelContext;

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.camel.trace.enabled", "true");
        props.setProperty("quarkus.camel.trace.backlog-size", "100");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    @Test
    public void testTraceConfiguration() {
        BacklogTracer tracer = getBacklogTracer(camelContext);

        assertThat(camelContext.isBacklogTracing()).isTrue();
        assertThat(camelContext.isBacklogTracingStandby()).isFalse();
        assertThat(camelContext.isBacklogTracingTemplates()).isFalse();
        assertThat(tracer).isNotNull();
        assertThat(tracer.getBacklogSize()).isEqualTo(100);
        assertThat(tracer.getTracePattern()).isNull();
        assertThat(tracer.getTraceFilter()).isNull();
    }

    private BacklogTracer getBacklogTracer(CamelContext camelContext) {
        BacklogTracer tracer = null;
        if (registry != null) {
            // lookup in registry
            Map<String, BacklogTracer> map = registry.findByTypeWithName(BacklogTracer.class);
            if (map.size() == 1) {
                tracer = map.values().iterator().next();
            }
        }
        if (tracer == null) {
            tracer = camelContext.getCamelContextExtension().getContextPlugin(BacklogTracer.class);
        }
        return tracer;
    }
}
