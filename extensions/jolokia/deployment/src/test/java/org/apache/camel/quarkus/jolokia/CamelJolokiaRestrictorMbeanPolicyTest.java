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

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamelJolokiaRestrictorMbeanPolicyTest {

    private static final String JOLOKIA_ACCESS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <restrict>
                <deny>
                    <mbean>
                        <name>java.lang:type=Runtime</name>
                        <attribute>Uptime</attribute>
                    </mbean>
                    <mbean>
                        <name>java.lang:type=Memory</name>
                        <operation>gc</operation>
                    </mbean>
                </deny>
                <filter>
                    <mbean>java.nio:type=BufferPool,*</mbean>
                </filter>
            </restrict>
            """;

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(JOLOKIA_ACCESS_XML), "jolokia-access.xml"));

    @Test
    void deniedAttributeBlocked() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isAttributeReadAllowed(new ObjectName("java.lang:type=Runtime"), "Uptime"));
    }

    @Test
    void otherAttributesOnSameMbeanAllowed() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isAttributeReadAllowed(new ObjectName("java.lang:type=Runtime"), "Name"));
    }

    @Test
    void deniedOperationBlocked() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOperationAllowed(new ObjectName("java.lang:type=Memory"), "gc"));
    }

    @Test
    void mbeanDomainFilteringStillEnforced() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isAttributeReadAllowed(new ObjectName("com.example:type=Test"), "Value"));
        assertTrue(restrictor.isObjectNameHidden(new ObjectName("com.example:type=Test")));
    }

    @Test
    void policyFilteredObjectNameHidden() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isObjectNameHidden(new ObjectName("java.nio:type=BufferPool,name=direct")));
        assertFalse(restrictor.isObjectNameHidden(new ObjectName("java.lang:type=Runtime")));
    }
}
