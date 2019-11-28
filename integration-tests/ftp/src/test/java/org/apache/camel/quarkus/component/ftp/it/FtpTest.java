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
package org.apache.camel.quarkus.component.ftp.it;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.ftpserver.usermanager.impl.WriteRequest;
import org.apache.mina.util.AvailablePortFinder;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class FtpTest {
    private static final Logger LOG = Logger.getLogger(FtpTest.class);

    private static final Path FTP_ROOT_DIR = Paths.get("target/ftp");
    private static final Path USERS_FILE = Paths.get("target/users.properties");
    private static final int PORT = AvailablePortFinder.getNextAvailable();

    private static FtpServer ftpServer;

    @BeforeAll
    public static void beforeAll() throws Exception {
        File usersFile = USERS_FILE.toFile();
        usersFile.createNewFile();

        NativeFileSystemFactory fsf = new NativeFileSystemFactory();
        fsf.setCreateHome(true);

        PropertiesUserManagerFactory pumf = new PropertiesUserManagerFactory();
        pumf.setAdminName("admin");
        pumf.setPasswordEncryptor(new ClearTextPasswordEncryptor());
        pumf.setFile(usersFile);

        UserManager userMgr = pumf.createUserManager();

        BaseUser user = new BaseUser();
        user.setName("admin");
        user.setPassword("admin");
        user.setHomeDirectory(FTP_ROOT_DIR.toString());

        List<Authority> authorities = new ArrayList<>();
        WritePermission writePermission = new WritePermission();
        writePermission.authorize(new WriteRequest());
        authorities.add(writePermission);
        user.setAuthorities(authorities);
        userMgr.save(user);

        ListenerFactory factory = new ListenerFactory();
        factory.setPort(PORT);

        FtpServerFactory serverFactory = new FtpServerFactory();
        serverFactory.setUserManager(userMgr);
        serverFactory.setFileSystem(fsf);
        serverFactory.setConnectionConfig(new ConnectionConfigFactory().createConnectionConfig());
        serverFactory.addListener("default", factory.createListener());

        FtpServerFactory ftpServerFactory = serverFactory;
        ftpServer = ftpServerFactory.createServer();
        ftpServer.start();
    }

    @AfterAll
    public static void afterAll() {
        if (ftpServer != null) {
            try {
                ftpServer.stop();
            } catch (Exception e) {
                LOG.warn("Failed to stop FTP server due to {}", e);
            }
        }
    }

    @Test
    public void testFtpComponent() throws InterruptedException {
        // Create a new file on the FTP server
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Hello Camel Quarkus FTP")
                .post("/ftp/create/hello.txt?port=" + PORT)
                .then()
                .statusCode(201);

        // Read file back from the FTP server
        RestAssured.get("/ftp/get/hello.txt?port=" + PORT)
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus FTP"));
    }

}
