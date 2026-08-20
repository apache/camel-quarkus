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
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Jolokia access policy that declares no allow-origin rule permits every origin. Delegating to it would turn
 * the deny by default stance into allow all for anybody who adds a policy purely to widen the remote rules, so
 * the default origin handling is kept instead.
 */
class CamelJolokiaRestrictorPolicyWithoutCorsTest {

    private static final String JOLOKIA_ACCESS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <restrict>
                <remote>
                    <host>10.0.0.0/8</host>
                </remote>
            </restrict>
            """;

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(JOLOKIA_ACCESS_XML), "jolokia-access.xml"));

    @Test
    void remoteRulesStillDelegatedToPolicy() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("10.0.0.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("192.168.1.1"));
    }

    @Test
    void crossOriginRequestsStillDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("http://untrusted.example", true));
        assertFalse(restrictor.isOriginAllowed("http://untrusted.example@localhost", true));
    }

    @Test
    void loopbackAndNullOriginsAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed(null, true));
        assertTrue(restrictor.isOriginAllowed("http://localhost:8080", true));
        assertTrue(restrictor.isOriginAllowed("http://127.0.0.1:9090", true));
    }
}
