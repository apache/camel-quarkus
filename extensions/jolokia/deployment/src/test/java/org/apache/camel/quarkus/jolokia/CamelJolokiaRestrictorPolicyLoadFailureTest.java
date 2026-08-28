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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import io.quarkus.test.QuarkusExtensionTest;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jolokia.server.core.util.HttpMethod;
import org.jolokia.server.core.util.RequestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A policy that is there but cannot be parsed denies everything, rather than being treated as absent. A policy
 * exists to restrict something, so silently carrying on without it would apply the opposite of what was asked
 * for. This is distinct from a location that points at nothing, which fails startup instead.
 */
class CamelJolokiaRestrictorPolicyLoadFailureTest {

    private static final String MALFORMED_JOLOKIA_ACCESS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <restrict>
                <remote>
                    <host>10.0.0.0/8</host>
            </restrict>
            """;

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(MALFORMED_JOLOKIA_ACCESS_XML), "jolokia-access.xml"))
            .overrideConfigKey("quarkus.camel.jolokia.additional-properties.policyLocation",
                    "classpath:/jolokia-access.xml")
            .overrideConfigKey("quarkus.camel.jolokia.remote-access-allowed", "true");

    @Test
    void unloadablePolicyDeniesEverything() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("127.0.0.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("10.0.0.1"));
        assertFalse(restrictor.isOriginAllowed(null, true));
        assertFalse(restrictor.isOriginAllowed("http://localhost:8080", false));
        assertFalse(restrictor.isHttpMethodAllowed(HttpMethod.GET));
        assertFalse(restrictor.isTypeAllowed(RequestType.READ));
    }

    @Test
    void unloadablePolicyDeniesMbeanAccess() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        ObjectName camelMBean = new ObjectName("org.apache.camel:context=camel-1,type=context,name=\"camel-1\"");
        assertFalse(restrictor.isAttributeReadAllowed(camelMBean, "Uptime"));
        assertFalse(restrictor.isAttributeWriteAllowed(camelMBean, "Tracing"));
        assertFalse(restrictor.isOperationAllowed(camelMBean, "sendStringBody"));
        assertTrue(restrictor.isObjectNameHidden(camelMBean));
    }
}
