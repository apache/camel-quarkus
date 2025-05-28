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
package oracle.as.jmx.framework;

import javax.management.MBeanServer;

/**
 * Hack to avoid 'Detected a MBean server in the image heap'.
 * The whole stacktrace contains static methods
 * at com.sun.jmx.mbeanserver.JmxMBeanServer.<init>(JmxMBeanServer.java:225)
 * at com.sun.jmx.mbeanserver.JmxMBeanServer.newMBeanServer(JmxMBeanServer.java:1437)
 * at javax.management.MBeanServerBuilder.newMBeanServer(MBeanServerBuilder.java:110)
 * at javax.management.MBeanServerFactory.newMBeanServer(MBeanServerFactory.java:329)
 * at javax.management.MBeanServerFactory.createMBeanServer(MBeanServerFactory.java:231)
 * at javax.management.MBeanServerFactory.createMBeanServer(MBeanServerFactory.java:192)
 * at java.lang.management.ManagementFactory.getPlatformMBeanServer(ManagementFactory.java:487)
 * at oracle.jdbc.driver.OracleDriver.registerMBeans(OracleDriver.java:494)
 * (Except javax.management.MBeanServerBuilder.newMBeanServer(MBeanServerBuilder.java:110), but substitution does not
 * help - should work!)
 *
 * If Oracle detects this class, the creation of MBeanServer is delegated to it.
 * Without MBeanServer, there is a warning `WARNING [ora.jdbc] Unable to find an MBeanServer so no MBears are
 * registered.`
 * and the native build proceeds.
 *
 */
public class PortableMBeanFactory {

    public MBeanServer getMBeanServer() {
        return null;
    }
}
