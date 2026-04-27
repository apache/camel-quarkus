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
package org.apache.camel.quarkus.test.support.sftp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * SFTP test resource that presents host certificates for host certificate verification testing.
 * Uses containerized OpenSSH server configured with host certificates.
 */
public class SftpHostCertTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(SftpHostCertTestResource.class);
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final int SFTP_PORT = 2222;

    private GenericContainer<?> container;
    private Path tempDir;
    private static SftpCertificates certificates;

    @Override
    public Map<String, String> start() {
        try {
            // Read configuration at runtime
            String opensshImage = ConfigProvider.getConfig().getValue("openssh-server.container.image", String.class);

            // Create temp directory for SSH configuration
            tempDir = Files.createTempDirectory("sftp-hostcert-config-");
            Path sshDir = tempDir.resolve("ssh");
            Files.createDirectories(sshDir);

            // Generate certificates
            certificates = SftpCertificates.generate(sshDir);
            LOGGER.info("Generated SSH certificates with host cert in: " + sshDir);

            // Create custom sshd_config that uses host certificate and trusts user CA
            Path sshdConfigPath = createSshdConfig(sshDir);

            // Start OpenSSH container with host certificate configuration
            container = new GenericContainer<>(opensshImage)
                    .withExposedPorts(SFTP_PORT)
                    .withEnv("PASSWORD_ACCESS", "true")
                    .withEnv("USER_NAME", USERNAME)
                    .withEnv("USER_PASSWORD", PASSWORD)
                    .withEnv("SUDO_ACCESS", "false")
                    // Copy host certificate and key
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(certificates.getHostPrivateKeyPath()),
                            "/config/ssh_host_ed25519_key")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(certificates.getHostCertificatePath()),
                            "/config/ssh_host_ed25519_key-cert.pub")
                    // Copy user CA public key for user cert verification
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(certificates.getUserCaPubKeyPath()),
                            "/config/user_ca.pub")
                    // Copy custom sshd_config
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(sshdConfigPath),
                            "/etc/ssh/sshd_config")
                    .waitingFor(Wait.forLogMessage(".*done.*", 1));

            container.start();

            Map<String, String> result = new HashMap<>();
            result.put("camel.sftp.hostcert.test-port", container.getMappedPort(SFTP_PORT).toString());

            // Set system property for JVM mode AND return in map for native mode command-line args
            String hostCaPubKeyPath = certificates.getHostCaPubKeyPath().toString();
            System.setProperty("sftp.test.host.ca.pubkey", hostCaPubKeyPath);
            result.put("sftp.test.host.ca.pubkey", hostCaPubKeyPath);

            LOGGER.infof("SFTP host cert container started on port %d", container.getMappedPort(SFTP_PORT));
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to start SFTP host cert container", e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
                LOGGER.info("SFTP host cert container stopped");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to stop SFTP host cert container", e);
        }

        try {
            if (tempDir != null && Files.exists(tempDir)) {
                Files.walk(tempDir)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                LOGGER.warn("Failed to delete temp file: " + path, e);
                            }
                        });
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to delete temp directory: " + tempDir, e);
        }
    }

    /**
     * Create custom sshd_config that uses host certificate and trusts user CA.
     */
    private Path createSshdConfig(Path sshDir) throws IOException {
        Path sshdConfig = sshDir.resolve("sshd_config");
        String config = String.join("\n",
                "# Custom sshd_config for host certificate testing",
                "Port 2222",
                "Protocol 2",
                "HostKey /config/ssh_host_ed25519_key",
                "HostCertificate /config/ssh_host_ed25519_key-cert.pub",
                "",
                "# User certificate authentication",
                "TrustedUserCAKeys /config/user_ca.pub",
                "PubkeyAuthentication yes",
                "",
                "# Password authentication",
                "PasswordAuthentication yes",
                "PermitRootLogin no",
                "",
                "# SFTP subsystem",
                "Subsystem sftp internal-sftp",
                "",
                "# Logging",
                "SyslogFacility AUTH",
                "LogLevel INFO",
                "");

        Files.writeString(sshdConfig, config);
        return sshdConfig;
    }

    /**
     * Returns the CA public key in OpenSSH format for use in known_hosts @cert-authority entries.
     */
    public static String getHostCaPublicKey() {
        try {
            String path = System.getProperty("sftp.test.host.ca.pubkey");
            if (path == null) {
                throw new IllegalStateException("Host CA public key path not set. Test resource not started?");
            }
            return java.nio.file.Files.readString(java.nio.file.Path.of(path)).trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get host CA public key", e);
        }
    }
}
