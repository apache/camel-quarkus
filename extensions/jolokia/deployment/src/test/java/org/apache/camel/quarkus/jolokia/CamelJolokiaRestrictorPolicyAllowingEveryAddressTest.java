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
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the one case where a `remote` section is read as saying nothing.
 *
 * Whether a policy restricts addresses is settled by asking it about an address reserved for documentation, which
 * a policy granting every address matches as readily as one with no `remote` section at all. The two are the same
 * instruction written differently, and both leave `remote-access-allowed` to answer, which is the stricter
 * reading. An operator who meant every address says so with the property.
 *
 * If Jolokia ever exposes its network checker, or whether the section is present, this becomes exact and the
 * `10.0.0.1` assertion below flips.
 */
class CamelJolokiaRestrictorPolicyAllowingEveryAddressTest {

    private static final String JOLOKIA_ACCESS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <restrict>
                <remote>
                    <host>0.0.0.0/0</host>
                </remote>
            </restrict>
            """;

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(JOLOKIA_ACCESS_XML), "jolokia-access.xml"));

    @Test
    void readAsSayingNothingSoTheLoopbackDefaultApplies() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("127.0.0.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("10.0.0.1"));
    }
}
