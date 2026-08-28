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
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Extending the restrictor is the documented way of customising it, and the subclass resolves the access policy
 * in the same inherited constructor. The policy therefore has to be checked before the server is created for a
 * subclass too, since the constructor runs after the agent port is bound and throwing there would leave it held
 * for the lifetime of the JVM.
 *
 * The failure has to arrive from the pre-check rather than from Jolokia instantiating the restrictor, which is
 * what the message assertion distinguishes.
 */
class CamelJolokiaRestrictorSubclassUnresolvablePolicyTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(SubclassedRestrictor.class))
            .overrideConfigKey("quarkus.camel.jolokia.additional-properties.restrictorClass",
                    SubclassedRestrictor.class.getName())
            .overrideConfigKey("quarkus.camel.jolokia.additional-properties.policyLocation",
                    "classpath:/jolokia-access-not-packaged.xml")
            .assertException(t -> {
                assertTrue(t instanceof IllegalStateException,
                        "Expected the pre-check to throw directly, not Jolokia to fail creating the restrictor: " + t);
                assertTrue(t.getMessage().contains("could not be resolved"), t.getMessage());
            });

    @Test
    void applicationShouldNotStart() {
        fail("The application should not have started with an unresolvable access policy");
    }

    public static class SubclassedRestrictor extends CamelJolokiaRestrictor {
    }
}
