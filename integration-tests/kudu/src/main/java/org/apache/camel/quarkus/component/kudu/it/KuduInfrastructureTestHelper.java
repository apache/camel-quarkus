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
package org.apache.camel.quarkus.component.kudu.it;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

/**
 * In order to run Kudu integration tests, {@code KuduTest} and {@code KuduIT} should have access to:
 * 1) A Kudu master server needed to create a table and also to obtain the host/port of the associated tablet server
 * 2) A Kudu tablet server needed to insert and scan records
 *
 * As such, one solution could be to use a custom setup where Kudu servers run on the same network than integration
 * tests. Please note that Kudu servers are not able to run on Windows machine.
 * Another solution could be to use the container based setup where Kudu servers are managed by
 * {@code KuduTestResource}.
 *
 * A) How to run integration tests against a custom setup (advised when not running on top of OpenJDK):
 * Install Kudu master and tablet servers on the same network than integration tests.
 * Configure "camel.kudu.test.master.rpc-authority" in "application.properties", for instance:
 * camel.kudu.test.master.rpc-authority=kudu-master-hostname:7051
 * Run integration tests with mvn clean integration-test -P native
 *
 * B) How to run integration tests against the container based setup:
 * The container based setup should run out of the box as {@code KuduTestResource} runs master and tablet server
 * containers in a shared network.
 * Simply run integration tests with mvn clean integration-test -P native
 * Note that the test harness is NOT guaranteed to work when NOT running on top of OpenJDK.
 *
 * Troubleshooting the container based setup:
 * If a message like "Unknown host kudu-tserver" is issued, it may be that
 * {@link KuduInfrastructureTestHelper#overrideTabletServerHostnameResolution()}
 * is not working. Please try to manually override the tablet server hostname resolution on your Operating System.
 * For instance, adding an entry in /etc/hosts file like: "127.0.0.1 kudu-tserver"
 *
 * If a message like "Not enough live tablet server" is issued, it may be that the shared network setup by
 * {@code KuduTestResource} is not working. In this case please refer to links below for a possible workaround:
 * <a href="https://github.com/apache/camel-quarkus/issues/1206">
 * <a href="https://github.com/moby/moby/issues/32138">
 */
@ApplicationScoped
public class KuduInfrastructureTestHelper {

    private static final Logger LOG = Logger.getLogger(KuduInfrastructureTestHelper.class);
    static final String KUDU_TABLET_SERVER_HOSTNAME = "kudu-tserver";
    public static final String KUDU_AUTHORITY_CONFIG_KEY = "camel.kudu.test.master.rpc-authority";

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Attempting to override the kudu tablet server hostname resolution on application startup");
        KuduInfrastructureTestHelper.overrideTabletServerHostnameResolution();
    }

    public static void overrideTabletServerHostnameResolution() {
        try {
            // Warm up the InetAddress cache
            InetAddress.getByName("localhost");
            final Field cacheField = InetAddress.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            final Object cache = cacheField.get(null);
            final Method get = ConcurrentHashMap.class.getMethod("get", Object.class);
            final Object localHostCachedAddresses = get.invoke(cache, "localhost");
            final Method put = ConcurrentHashMap.class.getMethod("put", Object.class, Object.class);
            put.invoke(cache, KUDU_TABLET_SERVER_HOSTNAME, localHostCachedAddresses);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hack the kudu tablet server hostname resolution", e);
        }
    }
}
