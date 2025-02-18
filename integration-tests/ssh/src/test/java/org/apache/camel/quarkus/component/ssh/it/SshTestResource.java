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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

public class SshTestResource implements QuarkusTestResourceLifecycleManager {
    static final String USERNAME = "user01";
    static final String PASSWORD = "changeit";

    protected List<SshServer> servers = new LinkedList<>();

    @Override
    public Map<String, String> start() {
        try {
            SshServer basicServer = setupBasicSSHDServer();
            SshServer secureServer = setupSecureSSHDServer();
            SshServer secureServerED25519 = setupSecureSSHDServerWithED25519Algorithm();

            return Map.of(
                    "ssh.host", "localhost",
                    "ssh.port", String.valueOf(basicServer.getPort()),
                    "ssh.secured-port", String.valueOf(secureServer.getPort()),
                    "ssh.ed-port", String.valueOf(secureServerED25519.getPort()),
                    "ssh.username", USERNAME,
                    "ssh.password", PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SshServer setupBasicSSHDServer() throws IOException {
        SshServer server = SshServer.setUpDefaultServer();
        server.setPort(AvailablePortFinder.getNextAvailable());
        server.setCommandFactory(new TestEchoCommandFactory());
        server.setPasswordAuthenticator((username, password, session) -> true);
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        server.start();
        servers.add(server);
        return server;
    }

    private SshServer setupSecureSSHDServer() throws IOException {
        SshServer server = SshServer.setUpDefaultServer();
        server.setPort(AvailablePortFinder.getNextAvailable());
        server.setKeyPairProvider(new FileKeyPairProvider(Paths.get("target/certs/user01.key")));
        server.setCommandFactory(new TestEchoCommandFactory());
        server.setPasswordAuthenticator((username, password, session) -> true);
        server.setPublickeyAuthenticator((username, key, session) -> true);
        server.start();
        servers.add(server);
        return server;
    }

    private SshServer setupSecureSSHDServerWithED25519Algorithm() throws IOException {
        SshServer server = SshServer.setUpDefaultServer();
        server.setPort(AvailablePortFinder.getNextAvailable());
        server.setCommandFactory(new TestEchoCommandFactory());
        server.setPasswordAuthenticator((username, password, session) -> true);
        server.setPublickeyAuthenticator((username, key, session) -> true);

        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("edDSA/key_ed25519.pem")) {
            if (stream == null) {
                throw new RuntimeException("Failed to load edDSA/key_ed25519.pem");
            }

            Path path = Paths.get("target/key_ed25519.pem");
            Files.write(path, stream.readAllBytes());
            server.setKeyPairProvider(new FileKeyPairProvider(path));
        }

        server.start();
        servers.add(server);
        return server;
    }

    @Override
    public void stop() {
        servers.forEach(s -> {
            try {
                s.stop(true);
            } catch (Exception e) {
                // ignored
            }
        });
        AvailablePortFinder.releaseReservedPorts();
    }
}
