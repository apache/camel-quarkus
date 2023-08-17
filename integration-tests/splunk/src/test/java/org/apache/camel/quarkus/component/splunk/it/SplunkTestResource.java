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
package org.apache.camel.quarkus.component.splunk.it;

import java.time.Duration;
import java.util.Map;
import java.util.TimeZone;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.component.splunk.ProducerType;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class SplunkTestResource implements QuarkusTestResourceLifecycleManager {

    public static String TEST_INDEX = "testindex";
    private static final String SPLUNK_IMAGE_NAME = ConfigProvider.getConfig().getValue("splunk.container.image", String.class);
    private static final int REMOTE_PORT = 8089;
    private static final int WEB_PORT = 8000;
    private static final Logger LOG = Logger.getLogger(SplunkTestResource.class);

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {

        try {
            container = new GenericContainer<>(SPLUNK_IMAGE_NAME)
                    .withExposedPorts(REMOTE_PORT, SplunkResource.LOCAL_TCP_PORT, WEB_PORT)
                    .withEnv("SPLUNK_START_ARGS", "--accept-license")
                    .withEnv("SPLUNK_PASSWORD", "changeit")
                    .withEnv("SPLUNK_LICENSE_URI", "Free")
                    .withEnv("TZ", TimeZone.getDefault().getID())
                    .waitingFor(
                            Wait.forLogMessage(".*Ansible playbook complete.*\\n", 1)
                                    .withStartupTimeout(Duration.ofMinutes(5)));

            container.start();

            container.execInContainer("sudo", "sed", "-i", "s/allowRemoteLogin=requireSetPassword/allowRemoteLogin=always/",
                    "/opt/splunk/etc/system/default/server.conf");
            container.execInContainer("sudo", "sed", "-i", "s/enableSplunkdSSL = true/enableSplunkdSSL = false/",
                    "/opt/splunk/etc/system/default/server.conf");
            container.execInContainer("sudo", "sed", "-i", "s/minFreeSpace = 5000/minFreeSpace = 100/",
                    "/opt/splunk/etc/system/default/server.conf");

            container.execInContainer("sudo", "microdnf", "--nodocs", "update", "tzdata");//install tzdata package so we can specify tz other than UTC

            container.execInContainer("sudo", "./bin/splunk", "restart");
            container.execInContainer("sudo", "./bin/splunk", "add", "index", TEST_INDEX);
            container.execInContainer("sudo", "./bin/splunk", "add", "tcp", String.valueOf(SplunkResource.LOCAL_TCP_PORT),
                    "-sourcetype",
                    ProducerType.TCP.name());

            String banner = StringUtils.repeat("*", 50);
            LOG.info(banner);
            LOG.infof("Splunk UI running on: http://localhost:%d", container.getMappedPort(WEB_PORT));
            LOG.info(banner);

            return Map.of(
                    SplunkResource.PARAM_REMOTE_PORT, container.getMappedPort(REMOTE_PORT).toString(),
                    SplunkResource.PARAM_TCP_PORT, container.getMappedPort(SplunkResource.LOCAL_TCP_PORT).toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // Ignored
        }
    }
}
