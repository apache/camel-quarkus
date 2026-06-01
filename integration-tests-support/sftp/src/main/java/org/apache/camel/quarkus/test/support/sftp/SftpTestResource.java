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
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * SFTP test resource using containerized OpenSSH server.
 * Supports password authentication and SSH certificate-based authentication.
 */
public class SftpTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(SftpTestResource.class);
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final int SFTP_PORT = 2222;

    private GenericContainer<?> container;
    private Path tempDir;
    private static SftpCertificates certificates;

    @Override
    public Map<String, String> start() {
        try {
            String opensshImage = ConfigProvider.getConfig().getValue("openssh-server.container.image", String.class);

            // Create temp directory for SSH configuration
            tempDir = Files.createTempDirectory("sftp-config-");
            Path sshDir = tempDir.resolve("ssh");
            Files.createDirectories(sshDir);

            certificates = SftpCertificates.generate(sshDir);
            LOGGER.info("Generated SSH certificates in: " + sshDir);

            // Create custom sshd_config for certificate authentication
            Path sshdConfigPath = createSshdConfig(sshDir);

            // Create authorized_keys file with all public keys
            Path authorizedKeysPath = createAuthorizedKeys(sshDir);

            // Create combined trusted CA file (Ed25519 + RSA CAs)
            Path trustedCaPath = createTrustedCaKeys(sshDir);

            // Start OpenSSH container
            container = new GenericContainer<>(opensshImage)
                    .withExposedPorts(SFTP_PORT)
                    .withEnv("PASSWORD_ACCESS", "true")
                    .withEnv("USER_NAME", USERNAME)
                    .withEnv("USER_PASSWORD", PASSWORD)
                    .withEnv("SUDO_ACCESS", "false")
                    // Copy trusted user CAs (Ed25519 + RSA) for certificate verification
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(trustedCaPath),
                            "/config/.ssh/trusted_user_cas.pub")
                    // Copy authorized_keys with all public keys
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(authorizedKeysPath),
                            "/config/.ssh/authorized_keys")
                    // Copy custom sshd_config
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(sshdConfigPath),
                            "/etc/ssh/sshd_config")
                    .waitingFor(Wait.forLogMessage(".*done.*", 1));

            container.start();

            Map<String, String> result = new HashMap<>();
            result.put("camel.sftp.test-port", container.getMappedPort(SFTP_PORT).toString());

            // Set system properties for JVM mode AND return in map for native mode command-line args
            String userKeyPath = certificates.getUserPrivateKeyPath().toString();
            String userCertPath = certificates.getUserCertificatePath().toString();
            String userKeyRsaPath = certificates.getUserRsaPrivateKeyPath().toString();
            String userCertRsaPath = certificates.getUserRsaCertificatePath().toString();
            String ftpKeyPath = certificates.getFtpPrivateKeyPath().toString();
            String ftpEncryptedKeyPath = certificates.getFtpEncryptedPrivateKeyPath().toString();

            System.setProperty("sftp.test.user.key", userKeyPath);
            System.setProperty("sftp.test.user.cert", userCertPath);
            System.setProperty("sftp.test.user.key.rsa", userKeyRsaPath);
            System.setProperty("sftp.test.user.cert.rsa", userCertRsaPath);
            System.setProperty("sftp.test.ftp.key", ftpKeyPath);
            System.setProperty("sftp.test.ftp.encrypted.key", ftpEncryptedKeyPath);

            result.put("sftp.test.user.key", userKeyPath);
            result.put("sftp.test.user.cert", userCertPath);
            result.put("sftp.test.user.key.rsa", userKeyRsaPath);
            result.put("sftp.test.user.cert.rsa", userCertRsaPath);
            result.put("sftp.test.ftp.key", ftpKeyPath);
            result.put("sftp.test.ftp.encrypted.key", ftpEncryptedKeyPath);

            LOGGER.infof("SFTP container started on port %d", container.getMappedPort(SFTP_PORT));
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to start SFTP container", e);
        }
    }

    /**
     * Create custom sshd_config that enables certificate authentication.
     */
    private Path createSshdConfig(Path sshDir) throws IOException {
        Path sshdConfig = sshDir.resolve("sshd_config");
        String config = String.join("\n",
                "# Custom sshd_config for SFTP testing",
                "Port 2222",
                "Protocol 2",
                "",
                "# Host keys",
                "HostKey /config/ssh_host_rsa_key",
                "HostKey /config/ssh_host_ed25519_key",
                "",
                "# Authentication",
                "PubkeyAuthentication yes",
                "PasswordAuthentication yes",
                "PermitRootLogin no",
                "",
                "# User certificate authentication - trust certificates signed by Ed25519 and RSA CAs",
                "TrustedUserCAKeys /config/.ssh/trusted_user_cas.pub",
                "",
                "# Standard public key authentication uses authorized_keys",
                "AuthorizedKeysFile /config/.ssh/authorized_keys",
                "",
                "# SFTP subsystem",
                "Subsystem sftp internal-sftp",
                "",
                "# Logging",
                "SyslogFacility AUTH",
                "LogLevel DEBUG",
                "");

        Files.writeString(sshdConfig, config);
        LOGGER.debug("Created sshd_config: " + sshdConfig);
        return sshdConfig;
    }

    /**
     * Create trusted CA keys file with both Ed25519 and RSA CAs.
     */
    private Path createTrustedCaKeys(Path sshDir) throws IOException {
        Path trustedCaKeys = sshDir.resolve("trusted_user_cas.pub");

        String cas = Files.readString(certificates.getUserCaPubKeyPath()) +
                Files.readString(certificates.getUserCaRsaPubKeyPath());

        Files.writeString(trustedCaKeys, cas);
        LOGGER.debug("Created trusted_user_cas.pub with Ed25519 and RSA CAs");
        return trustedCaKeys;
    }

    /**
     * Create authorized_keys file with all public keys for testing.
     */
    private Path createAuthorizedKeys(Path sshDir) throws IOException {
        Path authorizedKeys = sshDir.resolve("authorized_keys");

        String keys = Files.readString(certificates.getUserPublicKeyPath()) +
                Files.readString(certificates.getFtpPublicKeyPath()) +
                Files.readString(certificates.getFtpEncryptedPublicKeyPath());

        Files.writeString(authorizedKeys, keys);
        LOGGER.debug("Created authorized_keys with " + 3 + " keys");
        return authorizedKeys;
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
                LOGGER.info("SFTP container stopped");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to stop SFTP container", e);
        }

        try {
            if (tempDir != null && Files.exists(tempDir)) {
                try (Stream<Path> pathStream = Files.walk(tempDir)) {
                    pathStream
                            .sorted((a, b) -> -a.compareTo(b))
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    LOGGER.warn("Failed to delete temp file: " + path, e);
                                }
                            });
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to delete temp directory: " + tempDir, e);
        }
    }
}
