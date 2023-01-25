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

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.main.CamelMain;
import org.apache.camel.quarkus.main.CamelMainEventBridge;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class CamelMainEventBridgeDisabledTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    CamelMain main;

    @Test
    public void camelMainEventBridgeNotConfigured() {
        // Since no CDI observers for main events are configured the event bridge should not be present
        assertFalse(main.getMainListeners().stream().anyMatch(mainListener -> mainListener
                .getClass()
                .equals(CamelMainEventBridge.class)));
    }

    public static class MyRoutes extends RouteBuilder {
        public static String ROUTE_ID = "myRoute";
        public static String FROM_ENDPOINT = "direct://start";

        @Override
        public void configure() throws Exception {
            from(FROM_ENDPOINT)
                    .routeId(ROUTE_ID)
                    .log("${body}");
        }
    }
}
