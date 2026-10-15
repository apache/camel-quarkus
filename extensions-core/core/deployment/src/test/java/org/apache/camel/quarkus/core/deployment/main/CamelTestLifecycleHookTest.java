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
package org.apache.camel.quarkus.core.deployment.main;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkus.test.QuarkusExtensionTest;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.quarkus.core.test.CamelTestLifecycleHook;
import org.apache.camel.quarkus.core.test.TestApplicationInfo;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class CamelTestLifecycleHookTest {
    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RecordingHook.class, Routes.class)
                    .addAsResource(new StringAsset(RecordingHook.class.getName()),
                            "META-INF/services/" + CamelTestLifecycleHook.class.getName()));

    @Test
    void hooksFireInOrderBeforeRoutesAreCreated() {
        assertThat(RecordingHook.EVENTS).containsExactly(
                "beforeConfigure routeDefinitions=0 routes=0 profile=none",
                "afterConfigure routeDefinitions=1 routes=0 profile=none",
                "beforeStart routeDefinitions=1 routes=0 profile=none");
    }

    public static class RecordingHook implements CamelTestLifecycleHook {
        static final List<String> EVENTS = new CopyOnWriteArrayList<>();

        @Override
        public void beforeConfigure(CamelContext context, TestApplicationInfo info) {
            record("beforeConfigure", context, info);
        }

        @Override
        public void afterConfigure(CamelContext context, TestApplicationInfo info) {
            record("afterConfigure", context, info);
        }

        @Override
        public void beforeStart(CamelContext context, TestApplicationInfo info) {
            record("beforeStart", context, info);
        }

        private void record(String event, CamelContext context, TestApplicationInfo info) {
            int routeDefinitions = ((ModelCamelContext) context).getRouteDefinitions().size();
            EVENTS.add(event + " routeDefinitions=" + routeDefinitions + " routes=" + context.getRoutes().size()
                    + " profile=" + info.testProfileClassName().orElse("none"));
        }
    }

    public static class Routes extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:start").log("${body}");
        }
    }
}
