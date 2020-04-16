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
 * B) How to run integration tests against the container based setup when NOT running on top of OpenJDK 8:
 * Comment @Disabled and @DisabledOnNativeImage annotations from {@code KuduTest} and {@code KuduIT}.
 * Override the ip resolution of the host "kudu-tserver" to 127.0.0.1, e.g. by adding an entry in /etc/hosts file as
 * below:
 * 127.0.0.1 kudu-tserver
 * Run integration tests with mvn clean integration-test -P native
 *
 * C) How to run integration tests against the container based setup when running on top of OpenJDK 8:
 * Comment @Disabled and @DisabledOnNativeImage annotations from {@code KuduTest} and {@code KuduIT}.
 * No extra setup is needed as {@code overrideKuduTabletServerResolutionInInetAddressCache} takes care of redirecting
 * the Kudu tablet server traffic toward localhost
 * Run integration tests with mvn clean integration-test -P native
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
            Field field = InetAddress.class.getDeclaredField("addressCache");
            field.setAccessible(true);
            Object addressCache = field.get(null);

            Method put = addressCache.getClass().getMethod("put", String.class, InetAddress[].class);
            put.setAccessible(true);
            put.invoke(addressCache, KUDU_TABLET_SERVER_HOSTNAME, (Object[]) InetAddress.getAllByName("localhost"));
        } catch (Exception ex) {
            final String msg = "Can't override the kudu tablet server hostname resolution when not running on top of OpenJDK 8";
            LOG.error(msg, ex);
        }
    }
}
