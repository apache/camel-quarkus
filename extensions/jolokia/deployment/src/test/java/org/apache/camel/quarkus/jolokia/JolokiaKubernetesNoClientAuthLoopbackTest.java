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

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Turning SSL client authentication off on Kubernetes is an explicit opt-out, so nothing authenticates clients
 * and the agent falls back to the same loopback defaults it uses everywhere else. It must not inherit the
 * Kubernetes bind address or the lifted address restriction.
 */
class JolokiaKubernetesNoClientAuthLoopbackTest {
    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("kubernetes.service.host", "fake-host")
            .overrideConfigKey("quarkus.camel.jolokia.kubernetes.client-authentication-enabled", "false");

    @Test
    void remoteAddressesStillDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("10.128.4.7"));
    }

    @Test
    void agentStillServesPlainHttpOnLoopback() {
        RestAssured.port = 8778;
        RestAssured.get("/jolokia/")
                .then()
                .statusCode(200)
                .body("status", equalTo(200));
    }
}
