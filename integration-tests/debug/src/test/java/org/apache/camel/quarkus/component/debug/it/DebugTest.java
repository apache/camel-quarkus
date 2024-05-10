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
package org.apache.camel.quarkus.component.debug.it;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.ServiceStatus;
import org.apache.camel.api.management.mbean.ManagedBacklogDebuggerMBean;
import org.junit.jupiter.api.Test;

import static org.apache.camel.impl.debugger.DebuggerJmxConnectorService.DEFAULT_HOST;
import static org.apache.camel.impl.debugger.DebuggerJmxConnectorService.DEFAULT_REGISTRY_PORT;
import static org.apache.camel.impl.debugger.DebuggerJmxConnectorService.DEFAULT_SERVICE_URL_PATH;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
public class DebugTest {

    @Test
    public void camelDebuggingEnabled() {
        RestAssured.get("/debug/enabled")
                .then()
                .body(is("true"))
                .statusCode(200);
    }

    @Test
    void camelDebugJmxConnection() throws Exception {
        String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%d%s", DEFAULT_HOST, DEFAULT_REGISTRY_PORT,
                DEFAULT_SERVICE_URL_PATH);
        JMXServiceURL jmxUrl = new JMXServiceURL(url);

        await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            try (JMXConnector connector = JMXConnectorFactory.connect(jmxUrl)) {
                MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();

                ObjectName objectName = new ObjectName("org.apache.camel:type=context,*");
                Set<ObjectInstance> mbeans = mbeanServer.queryMBeans(objectName, null);
                assertNotNull(mbeans);

                Iterator<ObjectInstance> iterator = mbeans.iterator();
                if (iterator.hasNext()) {
                    ObjectInstance camelContext = iterator.next();
                    assertNotNull(camelContext);

                    String status = (String) mbeanServer.invoke(camelContext.getObjectName(), "getState", new Object[] {},
                            new String[] {});
                    assertEquals(ServiceStatus.Started, ServiceStatus.valueOf(status));
                } else {
                    fail("Expected to find 1 CamelContext MBean");
                }
            }
        });
    }

    @Test
    void accessToBacklogDebugger() throws Exception {
        String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%d%s", DEFAULT_HOST, DEFAULT_REGISTRY_PORT,
                DEFAULT_SERVICE_URL_PATH);
        JMXServiceURL jmxUrl = new JMXServiceURL(url);

        await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            try (JMXConnector connector = JMXConnectorFactory.connect(jmxUrl)) {
                MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();

                ObjectName objectName = new ObjectName("org.apache.camel:context=*,type=tracer,name=BacklogDebugger");
                Set<ObjectName> names = mbeanServer.queryNames(objectName, null);
                assertNotNull(names);

                Iterator<ObjectName> iteratorNames = names.iterator();
                if (iteratorNames.hasNext()) {
                    ObjectName debuggerMBeanObjectName = iteratorNames.next();
                    assertNotNull(debuggerMBeanObjectName);
                    ManagedBacklogDebuggerMBean backlogDebugger = JMX.newMBeanProxy(mbeanServer, debuggerMBeanObjectName,
                            ManagedBacklogDebuggerMBean.class);
                    Set<String> breakpoints = backlogDebugger.breakpoints();
                    assertNotNull(breakpoints);
                    Set<String> suspendedBreakpointNodeIds = backlogDebugger.suspendedBreakpointNodeIds();
                    assertNotNull(suspendedBreakpointNodeIds);
                } else {
                    fail("Expected to find 1 BacklogDebugger");
                }
            }
        });
    }
}
