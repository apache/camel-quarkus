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
package org.apache.camel.quarkus.component.management.it;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ManagementTest {

    private MBeanServer server;

    @Inject
    ProducerTemplate template;

    @Inject
    CamelContext camelContext;

    @BeforeEach
    public void setUp() {
        server = ManagementFactory.getPlatformMBeanServer();
    }

    @ParameterizedTest
    @ValueSource(strings = { "components", "consumers", "context", "dataformats", "endpoints", "processors", "routes",
            "services" })
    public void testManagementObjects(String type) throws Exception {
        // Look up an object instance by type
        ObjectName objectName = new ObjectName("org.apache.camel:type=" + type + ",*");
        Set<ObjectInstance> mbeans = server.queryMBeans(objectName, null);
        assertTrue(mbeans.size() > 0);

        // The CamelId attribute is common to all managed Camel objects,
        // and should match the name of the CamelContext.
        ObjectInstance mbean = mbeans.iterator().next();
        String camelId = (String) server.getAttribute(mbean.getObjectName(), "CamelId");
        assertEquals(camelContext.getName(), camelId);
    }

    @Test
    public void testDumpRoutesAsXml() throws Exception {
        ObjectName objectName = new ObjectName("org.apache.camel:type=context,*");
        Set<ObjectInstance> mbeans = server.queryMBeans(objectName, null);
        assertEquals(1, mbeans.size());

        ObjectInstance instance = mbeans.iterator().next();
        String routeXML = (String) server.invoke(instance.getObjectName(), "dumpRoutesAsXml", new Object[] {}, new String[] {});
        assertTrue(routeXML.contains("<from uri=\"direct:start\"/>"));
    }

    @Test
    public void testManagedBean() throws Exception {
        ObjectName objectName = new ObjectName("org.apache.camel:type=processors,name=\"counter\",*");
        Set<ObjectInstance> mbeans = server.queryMBeans(objectName, null);
        assertEquals(1, mbeans.size());
        ObjectInstance instance = mbeans.iterator().next();

        // Counter should be initialized to 0
        Integer count = (Integer) server.getAttribute(instance.getObjectName(), "Count");
        assertEquals(0, count);

        // Calling the increment() method should set counter to 1
        server.invoke(instance.getObjectName(), "increment", new Object[] {}, new String[] {});
        count = (Integer) server.getAttribute(instance.getObjectName(), "Count");
        assertEquals(1, count);

        // Call the "direct:count" endpoint to increment the counter
        template.requestBody("direct:count", "");

        count = (Integer) server.getAttribute(instance.getObjectName(), "Count");
        assertEquals(2, count);
    }
}
