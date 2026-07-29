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
package org.apache.camel.quarkus.jolokia;

import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class JolokiaKubernetesTlsFailureTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("kubernetes.service.host", "fake-host")
            .overrideConfigKey("quarkus.camel.jolokia.kubernetes.service-ca-cert", "/non/existent/ca.crt")
            .overrideConfigKey("quarkus.camel.jolokia.server.host", "0.0.0.0")
            .assertException(t -> {
                if (t.getMessage() == null
                        || !t.getMessage().contains("Kubernetes SSL client authentication is enabled")) {
                    throw new AssertionError(
                            "Expected RuntimeException about Kubernetes SSL client authentication failure, got: " + t, t);
                }
            });

    @Test
    void applicationShouldFailToStart() {
        // The application should not start — the assertException above validates the failure
    }
}
