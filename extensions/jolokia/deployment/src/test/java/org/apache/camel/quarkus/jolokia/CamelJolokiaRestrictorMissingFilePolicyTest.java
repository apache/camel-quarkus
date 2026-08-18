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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A `file:` policy that is not there fails startup like a `classpath:` one that is not packaged. Both are the
 * same packaging error, and a ConfigMap that failed to mount is the likeliest way to arrive at it.
 *
 * Denying every request instead would start the application and answer with "No access from client [chain:
 * 127.0.0.1] allowed", which points at the loopback control rather than at the missing file.
 */
class CamelJolokiaRestrictorMissingFilePolicyTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.camel.jolokia.additional-properties.policyLocation",
                    "file:/etc/jolokia/not-mounted.xml")
            .assertException(t -> {
                assertTrue(t instanceof IllegalStateException, "Expected the pre-check to throw, got: " + t);
                assertTrue(t.getMessage().contains("could not be resolved"), t.getMessage());
                assertTrue(t.getMessage().contains("not-mounted.xml"), t.getMessage());
            });

    @Test
    void applicationShouldNotStart() {
        fail("The application should not have started with a policy location that does not exist");
    }
}
