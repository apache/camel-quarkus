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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.config.keys.OpenSshCertificate;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.signature.BuiltinSignatures;
import org.apache.sshd.common.signature.Signature;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.jboss.logging.Logger;

/**
 * SFTP test resource that presents host certificates for host certificate verification testing.
 * This is separate from SftpTestResource to avoid interfering with other tests.
 */
public class SftpHostCertTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(SftpHostCertTestResource.class);

    private SshServer sshServer;
    private Path sftpHome;

    @Override
    public Map<String, String> start() {
        try {
            final int port = AvailablePortFinder.getNextAvailable();

            sftpHome = Files.createTempDirectory("sftp-hostcert-");
            Path adminHome = sftpHome.resolve("admin");
            Files.createDirectories(adminHome);

            VirtualFileSystemFactory factory = new VirtualFileSystemFactory();
            factory.setUserHomeDir("admin", adminHome.toAbsolutePath());

            sshServer = SshServer.setUpDefaultServer();
            sshServer.setPort(port);

            sshServer.setKeyPairProvider(createHostCertKeyPairProvider());

            sshServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
            sshServer.setCommandFactory(new ScpCommandFactory());
            sshServer.setPasswordAuthenticator((username, password, session) -> true);
            sshServer.setPublickeyAuthenticator((username, key, session) -> true);

            // Add certificate signature factories
            List<NamedFactory<Signature>> signatureFactories = sshServer.getSignatureFactories();
            signatureFactories.add(BuiltinSignatures.rsa_cert);
            signatureFactories.add(BuiltinSignatures.rsaSHA256_cert);
            signatureFactories.add(BuiltinSignatures.rsaSHA512_cert);
            signatureFactories.add(BuiltinSignatures.ed25519_cert);
            sshServer.setSignatureFactories(signatureFactories);

            sshServer.setFileSystemFactory(factory);
            sshServer.start();

            LOGGER.infof("SFTP server with host certificate started on port %d", port);

            return Collections.singletonMap("camel.sftp.hostcert.test-port", Integer.toString(port));
        } catch (Exception e) {
            throw new RuntimeException("Failed to start SFTP server with host certificate", e);
        }
    }

    @Override
    public void stop() {
        try {
            if (sshServer != null) {
                sshServer.stop();
                LOGGER.info("SFTP server with host certificate stopped");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to stop SFTP server", e);
        }

        try {
            if (sftpHome != null && Files.exists(sftpHome)) {
                Files.walk(sftpHome)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception e) {
                                LOGGER.warn("Failed to delete SFTPHome file", e);
                            }
                        });
            }
        } catch (Exception e) {
            LOGGER.warnf("Failed to delete sftp home: %s, %s", sftpHome, e);
        }

        AvailablePortFinder.releaseReservedPorts();
    }

    /**
     * Creates a KeyPairProvider that wraps the host private key with the OpenSSH host certificate.
     */
    private static KeyPairProvider createHostCertKeyPairProvider() {
        try {
            // Load host private key from classpath (works in both JVM and native mode)
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // Write host key to temp file (FileKeyPairProvider needs a file path)
            Path tempHostKey = Files.createTempFile("host-key-rsa", ".key");
            try (var keyStream = classLoader.getResourceAsStream("certs/host-key-rsa.key")) {
                if (keyStream == null) {
                    throw new IllegalStateException("Host key resource not found: certs/host-key-rsa.key");
                }
                Files.write(tempHostKey, keyStream.readAllBytes());
            }

            FileKeyPairProvider keyProvider = new FileKeyPairProvider(tempHostKey);
            KeyPair originalKeyPair = keyProvider.loadKeys(null).iterator().next();

            // Load host certificate from classpath
            String certLine;
            try (var certStream = classLoader.getResourceAsStream("certs/host-key-rsa-cert.pub")) {
                if (certStream == null) {
                    throw new IllegalStateException("Host certificate resource not found: certs/host-key-rsa-cert.pub");
                }
                certLine = new String(certStream.readAllBytes()).trim();
            }

            PublicKey certKey = PublicKeyEntry.parsePublicKeyEntry(certLine).resolvePublicKey(null, null, null);

            if (!(certKey instanceof OpenSshCertificate)) {
                throw new IllegalStateException("Host certificate file does not contain an OpenSSH certificate");
            }

            // Create a key pair with the certificate as the public key
            KeyPair certKeyPair = new KeyPair(certKey, originalKeyPair.getPrivate());

            // Clean up temp file
            Files.deleteIfExists(tempHostKey);

            return KeyPairProvider.wrap(certKeyPair);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load host certificate key pair", e);
        }
    }
}
