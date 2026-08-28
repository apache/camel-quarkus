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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An explicitly configured classpath policy that does not resolve fails the application at startup, rather than
 * falling back to the remote-access-allowed handling. Without this, a native executable built without
 * registering the policy as a resource would silently run with no policy at all, and with remote-access-allowed
 * set that means every origin and every MBean operation is permitted.
 */
class CamelJolokiaRestrictorUnresolvablePolicyTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.camel.jolokia.additional-properties.policyLocation",
                    "classpath:/jolokia-access-not-packaged.xml")
            .overrideConfigKey("quarkus.camel.jolokia.remote-access-allowed", "true")
            .assertException(t -> {
                IllegalStateException cause = null;
                for (Throwable current = t; current != null; current = current.getCause()) {
                    if (current instanceof IllegalStateException illegalState) {
                        cause = illegalState;
                        break;
                    }
                }
                assertNotNull(cause, "Expected an IllegalStateException in the cause chain, got: " + t);
                assertTrue(cause.getMessage().contains("jolokia-access-not-packaged.xml"), cause.getMessage());
                assertTrue(cause.getMessage().contains("quarkus.native.resources.includes"), cause.getMessage());
            });

    @Test
    void applicationShouldNotStart() {
        fail("The application should not have started with an unresolvable access policy");
    }
}
