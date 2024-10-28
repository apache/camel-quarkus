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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import io.quarkus.logging.Log;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jetbrains.annotations.NotNull;
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

    private String certName;
    private String caCertPath;
    private String certPath;
    private String certPrivateKey;
    private String keystorePassword;

    @Override
    public void init(Map<String, String> initArgs) {
        certName = initArgs.get("certName");
        if (StringUtils.isNotBlank(certName)) {
            caCertPath = initArgs.getOrDefault("caCertPath", "target/certs/%s-ca.crt".formatted(certName));
            certPath = initArgs.getOrDefault("caCertPath", "target/certs/%s.crt".formatted(certName));
            certPrivateKey = initArgs.getOrDefault("certPrivateKey", "target/certs/%s.key".formatted(certName));
            keystorePassword = initArgs.getOrDefault("keystorePassword", "password");
        }
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
                    .withEnv("TZ", TimeZone.getDefault().getID())
                    .waitingFor(
                            Wait.forLogMessage(".*Ansible playbook complete.*\\n", 1)
                                    .withStartupTimeout(Duration.ofMinutes(5)));

            if (certPath != null && caCertPath != null && keystorePassword != null) {
                //combine key + certificates into 1 pem - required for splunk
                //extraction of private key can not be done by keytool (only openssl), but it can be done programmatically
                byte[] concatenate = concatenateKeyAndCertificates(banner);

                container.withCopyToContainer(Transferable.of(concatenate), "/opt/splunk/etc/auth/mycerts/myServerCert.pem")
                        .withCopyToContainer(Transferable.of(Files.readAllBytes(Paths.get(caCertPath))),
                                "/opt/splunk/etc/auth/mycerts/cacert.pem");
            } else {
                LOG.debug("Internal certificates are used for Splunk server.");
            }

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

            /* uncomment for troubleshooting purposes - copy configuration from container
            container.copyFileFromContainer("/opt/splunk/etc/system/local/server.conf",
                    Path.of(getClass().getResource("/").getPath()).resolve("local_server_from_container.conf").toFile()
                            .getAbsolutePath());*/

            assertExecResult(container.execInContainer("sudo", "microdnf", "--nodocs", "update", "tzdata"), "tzdata install");//install tzdata package so we can specify tz other than UTC

            LOG.debug(banner);
            LOG.debug("Restarting splunk server.");
            LOG.debug(banner);

            assertExecResult(container.execInContainer("sudo", "./bin/splunk", "restart"), "splunk restart");

            container.execInContainer("sudo", "./bin/splunk", "add", "index", TEST_INDEX);
            container.execInContainer("sudo", "./bin/splunk", "add", "tcp", String.valueOf(SplunkConstants.TCP_PORT),
                    "-sourcetype", "TCP");

            /*uncomment for troubleshooting purposes - copy from container conf and log files
            container.copyFileFromContainer("/opt/splunk/etc/system/local/server.conf",
                    Path.of(getClass().getResource("/").getPath()).resolve("local-server-from-container.conf").toFile()
                            .getAbsolutePath());
            container.copyFileFromContainer("/opt/splunk/etc/system/default/server.conf",
                    Path.of(getClass().getResource("/").getPath()).resolve("default-server-from-container.log").toFile()
                            .getAbsolutePath());
            if (localhostCertPath != null && localhostKeystorePath != null && caCertPath != null && keystorePassword != null) {
                container.copyFileFromContainer("/opt/splunk/etc/auth/mycerts/myServerCert.pem",
                        Path.of(getClass().getResource("/").getPath()).resolve("myServerCert-from-container.pem").toFile()
                                .getAbsolutePath());
                container.copyFileFromContainer("/opt/splunk/etc/auth/mycerts/cacert.pem",
                        Path.of(getClass().getResource("/").getPath()).resolve("cacert-from-container.pem").toFile()
                                .getAbsolutePath());
            } else {
                container.copyFileFromContainer("/opt/splunk/etc/auth/server.pem",
                        Path.of(getClass().getResource("/").getPath()).resolve("myServerCert-from-container.pem").toFile()
                                .getAbsolutePath());
                container.copyFileFromContainer("/opt/splunk/etc/auth/cacert.pem",
                        Path.of(getClass().getResource("/").getPath()).resolve("cacert-from-container.pem").toFile()
                                .getAbsolutePath());
            }
            */

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
            LOG.debug(m.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).sorted()
                    .collect(Collectors.joining("\n")));
            LOG.debug(banner);

            return m;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte @NotNull [] concatenateKeyAndCertificates(String banner) throws IOException {
        // Encode the private key to PEM format
        String pemKey = Files.readString(Paths.get(certPrivateKey));

        // The server cert and the CA cert has to be concatenated
        String severCert = Files.readString(
                Paths.get(certPath),
                StandardCharsets.UTF_8);
        String ca = Files.readString(Paths.get(caCertPath),
                StandardCharsets.UTF_8);
        Log.debug("cacert content:");
        Log.debug(ca);
        Log.debug(banner);
        return (severCert + ca + pemKey).getBytes(StandardCharsets.UTF_8);
    }

    private static void assertExecResult(Container.ExecResult res, String cmd) {
        if (res.getExitCode() != 0) {
            LOG.error("Command: " + cmd);
            LOG.error("Stdout: " + res.getStdout());
            LOG.error("Stderr: " + res.getStderr());
            throw new RuntimeException("Command " + cmd + ") failed. " + res.getStdout());
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
