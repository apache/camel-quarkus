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
package org.apache.camel.quarkus.test.support.splunk;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.TimeZone;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

public class SplunkTestResource implements QuarkusTestResourceLifecycleManager {

    public static String TEST_INDEX = "testindex";
    public static final String HEC_TOKEN = "TESTTEST-TEST-TEST-TEST-TESTTESTTEST";

    private static final String SPLUNK_IMAGE_NAME = ConfigProvider.getConfig().getValue("splunk.container.image", String.class);
    private static final int REMOTE_PORT = 8089;
    private static final int WEB_PORT = 8000;
    private static final int HEC_PORT = 8088;
    private static final Logger LOG = LoggerFactory.getLogger(SplunkTestResource.class);

    private GenericContainer<?> container;

    private boolean ssl;
    private String localhostPemPath;
    private String caPemPath;

    @Override
    public void init(Map<String, String> initArgs) {
        ssl = Boolean.parseBoolean(initArgs.getOrDefault("ssl", "false"));
        localhostPemPath = initArgs.get("localhost_pem");
        caPemPath = initArgs.get("ca_pem");
    }

    @Override
    public Map<String, String> start() {

        String banner = StringUtils.repeat("*", 50);

        try {
            container = new GenericContainer<>(SPLUNK_IMAGE_NAME)
                    .withExposedPorts(REMOTE_PORT, SplunkConstants.TCP_PORT, WEB_PORT, HEC_PORT)
                    .withEnv("SPLUNK_START_ARGS", "--accept-license")
                    .withEnv("SPLUNK_PASSWORD", "changeit")
                    .withEnv("SPLUNK_HEC_TOKEN", HEC_TOKEN)
                    .withEnv("SPLUNK_LICENSE_URI", "Free")
                    .withEnv("SPLUNK_USER", "root")//does not work
                    .withEnv("TZ", TimeZone.getDefault().getID())
                    //                    .withLogConsumer(new Slf4jLogConsumer(LOG))
                    .waitingFor(
                            Wait.forLogMessage(".*Ansible playbook complete.*\\n", 1)
                                    .withStartupTimeout(Duration.ofMinutes(5)));

            if (ssl) {
                //localhost.pem and cacert.pem has to be concatenated
                String localhost = Files.readString(
                        Path.of(MountableFile.forClasspathResource(localhostPemPath).getResolvedPath()),
                        StandardCharsets.UTF_8);
                String ca = Files.readString(Path.of(MountableFile.forClasspathResource(caPemPath).getResolvedPath()),
                        StandardCharsets.UTF_8);
                byte[] concatenate = (localhost + "\n" + ca).getBytes(StandardCharsets.UTF_8);

                container.withCopyToContainer(Transferable.of(concatenate), "/opt/splunk/etc/auth/mycerts/myServerCert.pem")
                        .withCopyToContainer(MountableFile.forClasspathResource(caPemPath),
                                "/opt/splunk/etc/auth/mycerts/cacert.pem");
            }

            LOG.debug(banner);
            LOG.debug("Starting splunk server with ssl: " + ssl);
            LOG.debug(banner);
            container.start();

            container.copyFileToContainer(MountableFile.forClasspathResource("local_server.conf"),
                    "/opt/splunk/etc/system/local/server.conf");
            container.copyFileToContainer(MountableFile.forClasspathResource("local_inputs.conf"),
                    "/opt/splunk/etc/system/local/inputs.conf");

            container.copyFileToContainer(MountableFile.forClasspathResource("local_server.conf"),
                    "/opt/splunk/etc/system/local/server.conf");
            container.copyFileToContainer(MountableFile.forClasspathResource("local_inputs.conf"),
                    "/opt/splunk/etc/system/local/inputs.conf");

            container.execInContainer("sudo", "sed", "-i", "s/minFreeSpace = 5000/minFreeSpace = 100/",
                    "/opt/splunk/etc/system/local/server.conf");

            if (ssl) {
                //copy configuration for troubleshooting
                container.copyFileFromContainer("/opt/splunk/etc/system/local/server.conf",
                        Path.of(getClass().getResource("/").getPath()).resolve("local_server_from_container.conf").toFile()
                                .getAbsolutePath());
            } else {
                asserExecResult(
                        container.execInContainer("sudo", "sed", "-i", "s/enableSplunkdSSL = true/enableSplunkdSSL = false/",
                                "/opt/splunk/etc/system/default/server.conf"),
                        "disabling ssl");
            }

            container.execInContainer("sudo", "microdnf", "--nodocs", "update", "tzdata");//install tzdata package so we can specify tz other than UTC

            LOG.debug(banner);
            LOG.debug("Restarting splunk server.");
            LOG.debug(banner);
            asserExecResult(container.execInContainer("sudo", "./bin/splunk", "restart"), "splunk restart");

            container.execInContainer("sudo", "./bin/splunk", "add", "index", TEST_INDEX);
            container.execInContainer("sudo", "./bin/splunk", "add", "tcp", String.valueOf(SplunkConstants.TCP_PORT),
                    "-sourcetype", "TCP");

            String splunkHost = container.getHost();

            Map<String, String> m = Map.of(
                    SplunkConstants.PARAM_REMOTE_HOST, splunkHost,
                    SplunkConstants.PARAM_TCP_PORT, container.getMappedPort(SplunkConstants.TCP_PORT).toString(),
                    SplunkConstants.PARAM_HEC_TOKEN, HEC_TOKEN,
                    SplunkConstants.PARAM_TEST_INDEX, TEST_INDEX,
                    SplunkConstants.PARAM_REMOTE_PORT, container.getMappedPort(REMOTE_PORT).toString(),
                    SplunkConstants.PARAM_HEC_PORT, container.getMappedPort(HEC_PORT).toString());

            LOG.info(banner);
            LOG.info(String.format("Splunk UI running on: http://%s:%d", splunkHost, container.getMappedPort(WEB_PORT)));
            LOG.info(banner);
            LOG.debug(m.toString());
            LOG.debug(banner);

            return m;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void asserExecResult(Container.ExecResult res, String cmd) {
        if (res.getExitCode() != 0) {
            LOG.error("Command: " + cmd);
            LOG.error("Stdout: " + res.getStdout());
            LOG.error("Stderr: " + res.getStderr());
            throw new RuntimeException("ommand sed (serverCert) failed. " + res.getStdout());
        } else {
            LOG.debug("Command: " + cmd + " succeeded!");
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
