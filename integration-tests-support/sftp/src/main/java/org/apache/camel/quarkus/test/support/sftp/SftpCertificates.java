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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import org.apache.sshd.certificate.OpenSshCertificateBuilder;
import org.apache.sshd.common.config.keys.OpenSshCertificate;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.jboss.logging.Logger;

/**
 * Generates SSH certificates using Apache SSHD for testing purposes.
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

    // In-memory key pairs
    private KeyPair userCaKeyPair;
    private KeyPair userCaRsaKeyPair;
    private KeyPair userKeyPair;
    private KeyPair hostCaKeyPair;
    private KeyPair hostKeyPair;
    private KeyPair ftpKeyPair;
    private KeyPair ftpEncryptedKeyPair;
    private KeyPair userKeyRsaPair;

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
     * Generate all required SSH certificates using Apache SSHD.
     */
    public static SftpCertificates generate(Path sshDir) throws Exception {
        SftpCertificates certs = new SftpCertificates(sshDir);
        certs.generateAll();
        return certs;
    }

    private void generateAll() throws Exception {
        // Register BouncyCastle provider for Ed25519 support
        java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        SecureRandom random = new SecureRandom();

        userCaKeyPair = generateEd25519KeyPair();
        writeKeyPair(userCaKeyPair, userCaKeyPath, userCaPubKeyPath, "user-ca-ed25519", null);
        LOGGER.debug("Generated Ed25519 user CA key pair");

        userCaRsaKeyPair = generateRsaKeyPair(2048);
        writeKeyPair(userCaRsaKeyPair, userCaRsaKeyPath, userCaRsaPubKeyPath, "user-ca-rsa", null);
        LOGGER.debug("Generated RSA user CA key pair");

        userKeyPair = generateEd25519KeyPair();
        writeKeyPair(userKeyPair, userKeyPath, userPubKeyPath, "test-user-ed25519", null);
        LOGGER.debug("Generated Ed25519 user key pair");

        OpenSshCertificate userCert = OpenSshCertificateBuilder.userCertificate()
                .serial(random.nextLong() & Long.MAX_VALUE)
                .publicKey(userKeyPair.getPublic())
                .id("test-user-ed25519")
                .principals(Collections.singletonList("admin"))
                .validAfter(Instant.now().minus(Duration.ofMinutes(1)))
                .validBefore(Instant.now().plus(Duration.ofDays(365)))
                .sign(userCaKeyPair);
        writeCertificate(userCert, userCertPath, "test-user-ed25519");
        LOGGER.debug("Signed Ed25519 user certificate with Ed25519 CA");

        userKeyRsaPair = generateRsaKeyPair(2048);
        writeKeyPair(userKeyRsaPair, userKeyRsaPath, userPubKeyRsaPath, "test-user-rsa", null);
        LOGGER.debug("Generated RSA user key pair");

        OpenSshCertificate userCertRsa = OpenSshCertificateBuilder.userCertificate()
                .serial(random.nextLong() & Long.MAX_VALUE)
                .publicKey(userKeyRsaPair.getPublic())
                .id("test-user-rsa")
                .principals(Collections.singletonList("admin"))
                .validAfter(Instant.now().minus(Duration.ofMinutes(1)))
                .validBefore(Instant.now().plus(Duration.ofDays(365)))
                .sign(userCaRsaKeyPair, "rsa-sha2-512");
        writeCertificate(userCertRsa, userCertRsaPath, "test-user-rsa");
        LOGGER.debug("Signed RSA user certificate with RSA CA");

        hostCaKeyPair = generateEd25519KeyPair();
        writeKeyPair(hostCaKeyPair, hostCaKeyPath, hostCaPubKeyPath, "host-ca", null);
        LOGGER.debug("Generated host CA key pair");

        hostKeyPair = generateEd25519KeyPair();
        writeKeyPair(hostKeyPair, hostKeyPath, hostPubKeyPath, "sftp-server", null);
        LOGGER.debug("Generated host key pair");

        OpenSshCertificate hostCert = OpenSshCertificateBuilder.hostCertificate()
                .serial(random.nextLong() & Long.MAX_VALUE)
                .publicKey(hostKeyPair.getPublic())
                .id("sftp-server")
                .principals(Collections.singletonList("localhost"))
                .validAfter(Instant.now().minus(Duration.ofMinutes(1)))
                .validBefore(Instant.now().plus(Duration.ofDays(365)))
                .sign(hostCaKeyPair);
        writeCertificate(hostCert, hostCertPath, "sftp-server");
        LOGGER.debug("Signed host certificate");

        ftpKeyPair = generateRsaKeyPair(2048);
        writeKeyPair(ftpKeyPair, ftpKeyPath, ftpPubKeyPath, "ftp-test", null);
        LOGGER.debug("Generated ftp SSH key pair");

        ftpEncryptedKeyPair = generateRsaKeyPair(2048);
        writeKeyPair(ftpEncryptedKeyPair, ftpEncryptedKeyPath, ftpEncryptedPubKeyPath, "ftp-encrypted-test", "password");
        LOGGER.debug("Generated ftp-encrypted SSH key pair");
    }

    /**
     * Generate an Ed25519 key pair using BouncyCastle provider.
     */
    private KeyPair generateEd25519KeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519", "BC");
        return generator.generateKeyPair();
    }

    /**
     * Generate an RSA key pair with the specified key size.
     */
    private KeyPair generateRsaKeyPair(int keySize) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);
        return generator.generateKeyPair();
    }

    /**
     * Generate an ECDSA key pair with the specified curve.
     */
    private KeyPair generateEcdsaKeyPair(String curve) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec(curve));
        return generator.generateKeyPair();
    }

    /**
     * Write a key pair to files in OpenSSH format.
     *
     * @param keyPair        the key pair to write
     * @param privateKeyPath path for the private key file
     * @param publicKeyPath  path for the public key file
     * @param comment        comment to add to the keys
     * @param password       password for encrypting the private key (null for no encryption)
     */
    private void writeKeyPair(KeyPair keyPair, Path privateKeyPath, Path publicKeyPath, String comment, String password)
            throws IOException, GeneralSecurityException {
        OpenSSHKeyPairResourceWriter writer = OpenSSHKeyPairResourceWriter.INSTANCE;

        // Prepare encryption context if password is provided
        OpenSSHKeyEncryptionContext encryptionContext = null;
        if (password != null && !password.isEmpty()) {
            encryptionContext = new OpenSSHKeyEncryptionContext();
            encryptionContext.setPassword(password);
            encryptionContext.setCipherName("AES");
            encryptionContext.setCipherMode("CTR");
            encryptionContext.setCipherType("256");
        }

        // Write private key
        try (OutputStream out = Files.newOutputStream(privateKeyPath)) {
            writer.writePrivateKey(keyPair, comment, encryptionContext, out);
        }

        // Write public key in standard OpenSSH format
        String publicKeyLine = org.apache.sshd.common.config.keys.PublicKeyEntry.toString(keyPair.getPublic()) + " " + comment
                + "\n";
        Files.writeString(publicKeyPath, publicKeyLine);
    }

    /**
     * Write a certificate to file in OpenSSH format.
     *
     * @param certificate the certificate to write
     * @param certPath    path for the certificate file
     * @param comment     comment to add to the certificate
     */
    private void writeCertificate(OpenSshCertificate certificate, Path certPath, String comment)
            throws Exception {
        OpenSSHKeyPairResourceWriter writer = OpenSSHKeyPairResourceWriter.INSTANCE;
        try (OutputStream out = Files.newOutputStream(certPath)) {
            writer.writePublicKey(certificate, comment, out);
        }
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
