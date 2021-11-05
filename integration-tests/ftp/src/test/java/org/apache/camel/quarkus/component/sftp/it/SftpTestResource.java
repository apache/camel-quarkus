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
package org.apache.camel.quarkus.component.sftp.it;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.util.CollectionHelper;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.ClassLoadableResourceKeyPairProvider;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.jboss.logging.Logger;

public class SftpTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(SftpTestResource.class);

    private static final String KNOWN_HOSTS = "[localhost]:%d ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQDdfIWeSV4o68dRrKS"
            + "zFd/Bk51E65UTmmSrmW0O1ohtzi6HzsDPjXgCtlTt3FqTcfFfI92IlTr4JWqC9UK1QT1ZTeng0MkPQmv68hDANHbt5CpETZHjW5q4OOgWhV"
            + "vj5IyOC2NZHtKlJBkdsMAa15ouOOJLzBvAvbqOR/yUROsEiQ==";

    private SshServer sshServer;
    private Path sftpHome;
    private Path sshHome;

    @Override
    public Map<String, String> start() {
        try {
            final int port = AvailablePortFinder.getNextAvailable();

            sftpHome = Files.createTempDirectory("sftp-");
            sshHome = sftpHome.resolve("admin/.ssh");

            Files.createDirectories(sshHome);

            byte[] knownHostsBytes = String.format(KNOWN_HOSTS, port).getBytes(StandardCharsets.UTF_8);
            Files.write(sshHome.resolve(".known_hosts"), knownHostsBytes);

            VirtualFileSystemFactory factory = new VirtualFileSystemFactory();
            factory.setUserHomeDir("admin", sftpHome.resolve("admin").toAbsolutePath());

            sshServer = SshServer.setUpDefaultServer();
            sshServer.setPort(port);
            sshServer.setKeyPairProvider(new ClassLoadableResourceKeyPairProvider("hostkey.pem"));
            sshServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
            sshServer.setCommandFactory(new ScpCommandFactory());
            sshServer.setPasswordAuthenticator((username, password, session) -> true);
            sshServer.setPublickeyAuthenticator((username, key, session) -> true);
            sshServer.setFileSystemFactory(factory);
            sshServer.start();

            return CollectionHelper.mapOf("camel.sftp.test-port", Integer.toString(port));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (sshServer != null) {
                sshServer.stop();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to stop FTP server due to {}", e);
        }

        try {
            if (sftpHome != null) {
                Files.walk(sftpHome)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed delete sftp home: {}, {}", sftpHome, e);
        }
    }
}
