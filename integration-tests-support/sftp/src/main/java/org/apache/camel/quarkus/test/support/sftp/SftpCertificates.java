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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

/**
 * Generates SSH certificates using ssh-keygen for testing purposes.
 * Uses Ed25519 keys for both user and host certificates.
 */
public class SftpCertificates {
    private static final Logger LOGGER = Logger.getLogger(SftpCertificates.class);

    private final Path sshDir;
    private final Path userCaKeyPath;
    private final Path userCaPubKeyPath;
    private final Path userCaRsaKeyPath;
    private final Path userCaRsaPubKeyPath;
    private final Path userKeyPath;
    private final Path userPubKeyPath;
    private final Path userCertPath;
    private final Path hostCaKeyPath;
    private final Path hostCaPubKeyPath;
    private final Path hostKeyPath;
    private final Path hostPubKeyPath;
    private final Path hostCertPath;
    private final Path ftpKeyPath;
    private final Path ftpPubKeyPath;
    private final Path ftpEncryptedKeyPath;
    private final Path ftpEncryptedPubKeyPath;
    private final Path userKeyRsaPath;
    private final Path userPubKeyRsaPath;
    private final Path userCertRsaPath;

    private SftpCertificates(Path sshDir) {
        this.sshDir = sshDir;
        this.userCaKeyPath = sshDir.resolve("user_ca");
        this.userCaPubKeyPath = sshDir.resolve("user_ca.pub");
        this.userCaRsaKeyPath = sshDir.resolve("user_ca_rsa");
        this.userCaRsaPubKeyPath = sshDir.resolve("user_ca_rsa.pub");
        this.userKeyPath = sshDir.resolve("user_key");
        this.userPubKeyPath = sshDir.resolve("user_key.pub");
        this.userCertPath = sshDir.resolve("user_key-cert.pub");
        this.hostCaKeyPath = sshDir.resolve("host_ca");
        this.hostCaPubKeyPath = sshDir.resolve("host_ca.pub");
        this.hostKeyPath = sshDir.resolve("host_key");
        this.hostPubKeyPath = sshDir.resolve("host_key.pub");
        this.hostCertPath = sshDir.resolve("host_key-cert.pub");
        this.ftpKeyPath = sshDir.resolve("ftp.key");
        this.ftpPubKeyPath = sshDir.resolve("ftp.key.pub");
        this.ftpEncryptedKeyPath = sshDir.resolve("ftp-encrypted.key");
        this.ftpEncryptedPubKeyPath = sshDir.resolve("ftp-encrypted.key.pub");
        this.userKeyRsaPath = sshDir.resolve("user_key_rsa");
        this.userPubKeyRsaPath = sshDir.resolve("user_key_rsa.pub");
        this.userCertRsaPath = sshDir.resolve("user_key_rsa-cert.pub");
    }

    /**
     * Generate all required SSH certificates using ssh-keygen.
     */
    public static SftpCertificates generate(Path sshDir) throws IOException, InterruptedException {
        SftpCertificates certs = new SftpCertificates(sshDir);
        certs.generateAll();
        return certs;
    }

    private void generateAll() throws IOException, InterruptedException {
        // Generate Ed25519 user CA key pair
        runSshKeygen("-t", "ed25519", "-f", userCaKeyPath.toString(), "-N", "", "-C", "user-ca-ed25519");
        LOGGER.debug("Generated Ed25519 user CA key pair");

        // Generate RSA user CA key pair
        runSshKeygen("-t", "rsa", "-b", "2048", "-f", userCaRsaKeyPath.toString(), "-N", "", "-C", "user-ca-rsa");
        LOGGER.debug("Generated RSA user CA key pair");

        // Generate Ed25519 user key pair
        runSshKeygen("-t", "ed25519", "-f", userKeyPath.toString(), "-N", "", "-C", "test-user-ed25519");
        LOGGER.debug("Generated Ed25519 user key pair");

        // Sign Ed25519 user certificate with Ed25519 CA
        runSshKeygen("-s", userCaKeyPath.toString(),
                "-I", "test-user-ed25519",
                "-n", "admin",
                "-V", "-1m:+365d",
                userPubKeyPath.toString());
        LOGGER.debug("Signed Ed25519 user certificate with Ed25519 CA");

        // Generate RSA user key pair for certificate-based authentication
        runSshKeygen("-t", "rsa", "-b", "2048", "-f", userKeyRsaPath.toString(), "-N", "", "-C", "test-user-rsa");
        LOGGER.debug("Generated RSA user key pair");

        // Sign RSA user certificate with RSA CA
        runSshKeygen("-s", userCaRsaKeyPath.toString(),
                "-I", "test-user-rsa",
                "-n", "admin",
                "-V", "-1m:+365d",
                userPubKeyRsaPath.toString());
        LOGGER.debug("Signed RSA user certificate with RSA CA");

        // Generate host CA key pair
        runSshKeygen("-t", "ed25519", "-f", hostCaKeyPath.toString(), "-N", "", "-C", "host-ca");
        LOGGER.debug("Generated host CA key pair");

        // Generate host key pair
        runSshKeygen("-t", "ed25519", "-f", hostKeyPath.toString(), "-N", "", "-C", "sftp-server");
        LOGGER.debug("Generated host key pair");

        // Sign host certificate
        runSshKeygen("-s", hostCaKeyPath.toString(),
                "-I", "sftp-server",
                "-h",
                "-n", "localhost",
                "-V", "-1m:+365d",
                hostPubKeyPath.toString());
        LOGGER.debug("Signed host certificate");

        // Generate additional SSH key pairs for traditional public key authentication
        // These correspond to the ftp.key and ftp-encrypted.key files used by tests
        runSshKeygen("-t", "rsa", "-b", "2048", "-f", ftpKeyPath.toString(), "-N", "", "-C", "ftp-test");
        LOGGER.debug("Generated ftp SSH key pair");

        runSshKeygen("-t", "rsa", "-b", "2048", "-f", ftpEncryptedKeyPath.toString(), "-N", "password", "-C",
                "ftp-encrypted-test");
        LOGGER.debug("Generated ftp-encrypted SSH key pair");
    }

    private void runSshKeygen(String... args) throws IOException, InterruptedException {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "ssh-keygen";
        System.arraycopy(args, 0, cmd, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Capture output for debugging
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("ssh-keygen command timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("ssh-keygen failed with exit code " + exitCode + ": " + output);
        }

        LOGGER.debug("ssh-keygen output: " + output);
    }

    public Path getUserCaPubKeyPath() {
        return userCaPubKeyPath;
    }

    public Path getUserCaRsaPubKeyPath() {
        return userCaRsaPubKeyPath;
    }

    public Path getUserPublicKeyPath() {
        return userPubKeyPath;
    }

    public Path getUserPrivateKeyPath() {
        return userKeyPath;
    }

    public Path getUserCertificatePath() {
        return userCertPath;
    }

    public Path getHostCaPubKeyPath() {
        return hostCaPubKeyPath;
    }

    public Path getHostPublicKeyPath() {
        return hostPubKeyPath;
    }

    public Path getHostPrivateKeyPath() {
        return hostKeyPath;
    }

    public Path getHostCertificatePath() {
        return hostCertPath;
    }

    public byte[] getUserCertificateBytes() throws IOException {
        return Files.readAllBytes(userCertPath);
    }

    public byte[] getUserPrivateKeyBytes() throws IOException {
        return Files.readAllBytes(userKeyPath);
    }

    public String getHostCaPublicKey() throws IOException {
        return Files.readString(hostCaPubKeyPath).trim();
    }

    public Path getFtpPrivateKeyPath() {
        return ftpKeyPath;
    }

    public Path getFtpPublicKeyPath() {
        return ftpPubKeyPath;
    }

    public Path getFtpEncryptedPrivateKeyPath() {
        return ftpEncryptedKeyPath;
    }

    public Path getFtpEncryptedPublicKeyPath() {
        return ftpEncryptedPubKeyPath;
    }

    public Path getUserRsaPrivateKeyPath() {
        return userKeyRsaPath;
    }

    public Path getUserRsaPublicKeyPath() {
        return userPubKeyRsaPath;
    }

    public Path getUserRsaCertificatePath() {
        return userCertRsaPath;
    }

    public byte[] getUserRsaCertificateBytes() throws IOException {
        return Files.readAllBytes(userCertRsaPath);
    }

    public byte[] getUserRsaPrivateKeyBytes() throws IOException {
        return Files.readAllBytes(userKeyRsaPath);
    }
}
