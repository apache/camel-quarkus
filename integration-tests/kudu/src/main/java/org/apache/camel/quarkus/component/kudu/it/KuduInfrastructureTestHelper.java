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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;
import org.jboss.logging.Logger;

/**
 * In order to run Kudu integration tests, {@KuduResource}, {@code KuduTest} and {@code KuduIT} should have access to:
 * 1) A Kudu master server needed to create a table and also to obtain the host/port of the associated tablet server
 * 2) A Kudu tablet server needed to insert and scan records
 *
 * As such, one solution could be to use a custom setup where Kudu servers run on the same network than integration
 * tests. Please note that Kudu servers are not able to run on Windows machine.
 * Another solution could be to use the container based setup where Kudu servers are managed by
 * {@code KuduTestResource}.
 *
 * A) How to run integration tests against a custom setup:
 * Comment @Disabled and @DisabledOnNativeImage annotations from {@code KuduTest} and {@code KuduIT}.
 * Install Kudu master and tablet servers on the same network than integration tests.
 * Configure "camel.kudu.test.master.rpc-authority" in "application.properties", for instance:
 * camel.kudu.test.master.rpc-authority=kudu-master-hostname:7051
 * Run integration tests with mvn clean integration-test -P native
 *
 * B) How to run integration tests against the container based setup:
 * Comment @Disabled and @DisabledOnNativeImage annotations from {@code KuduTest} and {@code KuduIT}.
 * When NOT running on top of OpenJDK 9+, you'll need to manually override the ip resolution of the host "kudu-tserver"
 * to 127.0.0.1, e.g. by adding an entry in /etc/hosts file as below:
 * 127.0.0.1 kudu-tserver
 * Run integration tests with mvn clean integration-test -P native
 * Note that under some platforms/configurations, testcontainers is not able to bridge master and tablet servers in a
 * shared network.
 * In such cases, a log like "0 tablet servers are alive" could be issued and falling back to a custom local setup is
 * advised.
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
            Field cacheField = InetAddress.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(null);

            Method get = cache.getClass().getMethod("get", Object.class);
            Object localHostCachedAddresses = get.invoke(cache, "localhost");

            Method put = cache.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(cache, KUDU_TABLET_SERVER_HOSTNAME, localHostCachedAddresses);
        } catch (Exception ex) {
            final String msg = "An issue occurred while attempting the Open JDK9+ override of the kudu tablet server hostname resolution";
            LOG.warn(msg, ex);
        }
    }
}
