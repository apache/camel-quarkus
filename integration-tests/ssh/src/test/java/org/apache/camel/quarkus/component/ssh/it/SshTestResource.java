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
package org.apache.camel.quarkus.component.ssh.it;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class SshTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshTestResource.class);

    private static final int SSH_PORT = 2222;
    private static final String SSH_IMAGE = ConfigProvider.getConfig().getValue("openssh-server.container.image",
            String.class);
    static final String USERNAME = "user01";
    static final String PASSWORD = "changeit";

    private GenericContainer container;
    protected List<SshServer> sshds = new LinkedList<>();
    protected int securedPort, edPort;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());
        LOGGER.info("Starting SSH container");

        try {
            container = new GenericContainer(SSH_IMAGE)
                    .withExposedPorts(SSH_PORT)
                    .withEnv("PASSWORD_ACCESS", "true")
                    .withEnv("USER_NAME", USERNAME)
                    .withEnv("USER_PASSWORD", PASSWORD)
                    .waitingFor(Wait.forListeningPort());

            container.start();

            LOGGER.info("Started SSH container to {}:{}", container.getHost(),
                    container.getMappedPort(SSH_PORT).toString());

            securedPort = AvailablePortFinder.getNextAvailable();

            var sshd = SshServer.setUpDefaultServer();
            sshd.setPort(securedPort);
            sshd.setKeyPairProvider(new FileKeyPairProvider(Paths.get("target/certs/user01.key")));
            sshd.setCommandFactory(new TestEchoCommandFactory());
            sshd.setPasswordAuthenticator((username, password, session) -> true);
            sshd.setPublickeyAuthenticator((username, key, session) -> true);
            sshd.start();

            sshds.add(sshd);

            edPort = AvailablePortFinder.getNextAvailable();

            sshd = SshServer.setUpDefaultServer();
            sshd.setPort(edPort);

            File tmpFile = File.createTempFile("key_ed25519-", ".pem");
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("edDSA/key_ed25519.pem")) {
                Files.write(tmpFile.toPath(), in.readAllBytes());
                sshd.setKeyPairProvider(new FileKeyPairProvider(tmpFile.toPath()));
            } catch (NullPointerException | IOException e) {
                String message = "An issue occurred while loading key: edDSA/key_ed25519.pem";
                throw new RuntimeException(message, e);
            }

            sshd.setCommandFactory(new TestEchoCommandFactory());
            sshd.setPasswordAuthenticator((username, password, session) -> true);
            sshd.setPublickeyAuthenticator((username, key, session) -> true);
            sshd.start();

            sshds.add(sshd);

            LOGGER.info("Started SSHD server to {}:{}", container.getHost(),
                    securedPort);

            return Map.of(
                    "quarkus.ssh.host", "localhost",
                    "quarkus.ssh.port", container.getMappedPort(SSH_PORT).toString(),
                    "quarkus.ssh.secured-port", securedPort + "",
                    "quarkus.ssh.ed-port", edPort + "",
                    "ssh.username", USERNAME,
                    "ssh.password", PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        LOGGER.info("Stopping SSH container and servers");

        try {
            if (container != null) {
                container.stop();
            }
            sshds.forEach(s -> {
                try {
                    s.stop(true);
                } catch (Exception e) {
                    // ignored
                }
            });
        } catch (Exception e) {
            // ignored
        }
    }
}
