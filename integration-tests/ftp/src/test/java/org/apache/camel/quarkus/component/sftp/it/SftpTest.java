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

import io.quarkus.test.junit.DisabledOnNativeImage;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.mina.util.AvailablePortFinder;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.hamcrest.core.Is.is;

@QuarkusTest
class SftpTest {

    private static final Logger LOG = Logger.getLogger(SftpTest.class);

    private static final String KNOWN_HOSTS = "[localhost]:%d ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQDdfIWeSV4o68dRrKS"
            + "zFd/Bk51E65UTmmSrmW0O1ohtzi6HzsDPjXgCtlTt3FqTcfFfI92IlTr4JWqC9UK1QT1ZTeng0MkPQmv68hDANHbt5CpETZHjW5q4OOgWhV"
            + "vj5IyOC2NZHtKlJBkdsMAa15ouOOJLzBvAvbqOR/yUROsEiQ==";
    private static final int PORT = AvailablePortFinder.getNextAvailable();
    private static final String SFTP_HOME = "target/sftp";
    private static final String SSH_HOME = SFTP_HOME + "/admin/.ssh";

    private static SshServer server;

    @BeforeAll
    public static void beforeAll() throws Exception {
        new File(SSH_HOME).mkdirs();
        byte[] knownHostsBytes = String.format(KNOWN_HOSTS, PORT).getBytes(StandardCharsets.UTF_8);
        Files.write(Paths.get(SSH_HOME).resolve(".known_hosts"), knownHostsBytes);

        server = SshServer.setUpDefaultServer();
        server.setPort(PORT);
        server.setKeyPairProvider(new FileKeyPairProvider(Paths.get("src/test/resources/hostkey.pem")));
        server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        server.setCommandFactory(new ScpCommandFactory());
        server.setPasswordAuthenticator((username, password, session) -> true);
        server.setPublickeyAuthenticator((username, key, session) -> true);

        VirtualFileSystemFactory factory = new VirtualFileSystemFactory();
        factory.setUserHomeDir("admin", Paths.get(SFTP_HOME).resolve("admin").toAbsolutePath());
        server.setFileSystemFactory(factory);

        server.start();
    }

    @AfterAll
    public static void afterAll() {
        if (server != null) {
            try {
                server.stop(true);
            } catch (IOException e) {
                LOG.warn("Failed to stop SFTP server due to {}", e);
            }
        }
    }

    @Test
    @DisabledOnNativeImage("Disabled due to SSL native integration failing in the Jenkins CI environment." +
            "https://github.com/apache/camel-quarkus/issues/468")
    public void testSftpComponent() throws InterruptedException {
        // Create a new file on the SFTP server
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Hello Camel Quarkus SFTP")
                .post("/sftp/create/hello.txt?port=" + PORT)
                .then()
                .statusCode(201);

        // Read file back from the SFTP server
        RestAssured.get("/sftp/get/hello.txt?port=" + PORT)
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus SFTP"));
    }

}
