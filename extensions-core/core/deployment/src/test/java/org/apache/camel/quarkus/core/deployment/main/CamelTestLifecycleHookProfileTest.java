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

import io.quarkus.deployment.builditem.TestProfileBuildItem;
import io.quarkus.test.QuarkusExtensionTest;
import org.apache.camel.quarkus.core.deployment.main.CamelTestLifecycleHookTest.RecordingHook;
import org.apache.camel.quarkus.core.deployment.main.CamelTestLifecycleHookTest.Routes;
import org.apache.camel.quarkus.core.test.CamelTestLifecycleHook;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class CamelTestLifecycleHookProfileTest {
    private static final String PROFILE = "org.acme.MyTestProfile";

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .addBuildChainCustomizer(builder -> builder
                    .addBuildStep(context -> context.produce(new TestProfileBuildItem(PROFILE)))
                    .produces(TestProfileBuildItem.class)
                    .build())
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RecordingHook.class, Routes.class)
                    .addAsResource(new StringAsset(RecordingHook.class.getName()),
                            "META-INF/services/" + CamelTestLifecycleHook.class.getName()));

    @Test
    void hooksReceiveTestProfile() {
        assertThat(RecordingHook.EVENTS).containsExactly(
                "beforeConfigure routeDefinitions=0 routes=0 profile=" + PROFILE,
                "afterConfigure routeDefinitions=1 routes=0 profile=" + PROFILE,
                "beforeStart routeDefinitions=1 routes=0 profile=" + PROFILE);
    }
}
