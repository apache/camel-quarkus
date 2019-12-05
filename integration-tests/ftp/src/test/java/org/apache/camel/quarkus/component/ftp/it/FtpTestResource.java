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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.util.CollectionHelper;
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
import org.jboss.logging.Logger;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class FtpTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(FtpTestResource.class);

    private FtpServer ftpServer;
    private Path ftpRoot;
    private Path usrFile;

    @Override
    public Map<String, String> start() {
        try {
            final int port = AvailablePortFinder.getNextAvailable();

            ftpRoot = Files.createTempDirectory("ftp-");
            usrFile = Files.createTempFile("ftp-", ".properties");

            NativeFileSystemFactory fsf = new NativeFileSystemFactory();
            fsf.setCreateHome(true);

            PropertiesUserManagerFactory pumf = new PropertiesUserManagerFactory();
            pumf.setAdminName("admin");
            pumf.setPasswordEncryptor(new ClearTextPasswordEncryptor());
            pumf.setFile(usrFile.toFile());

            UserManager userMgr = pumf.createUserManager();

            BaseUser user = new BaseUser();
            user.setName("admin");
            user.setPassword("admin");
            user.setHomeDirectory(ftpRoot.toString());

            List<Authority> authorities = new ArrayList<>();
            WritePermission writePermission = new WritePermission();
            writePermission.authorize(new WriteRequest());
            authorities.add(writePermission);
            user.setAuthorities(authorities);
            userMgr.save(user);

            ListenerFactory factory = new ListenerFactory();
            factory.setPort(port);

            FtpServerFactory serverFactory = new FtpServerFactory();
            serverFactory.setUserManager(userMgr);
            serverFactory.setFileSystem(fsf);
            serverFactory.setConnectionConfig(new ConnectionConfigFactory().createConnectionConfig());
            serverFactory.addListener("default", factory.createListener());

            FtpServerFactory ftpServerFactory = serverFactory;
            ftpServer = ftpServerFactory.createServer();
            ftpServer.start();

            return CollectionHelper.mapOf(
                    "camel.ftp.test-port", Integer.toString(port),
                    "camel.ftp.test-root-dir", ftpRoot.toString(),
                    "camel.ftp.test-user-file", usrFile.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (ftpServer != null) {
                ftpServer.stop();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to stop FTP server due to {}", e);
        }

        try {
            if (ftpRoot != null) {
                Files.walk(ftpRoot)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed delete ftp root: {}, {}", ftpRoot, e);
        }

        try {
            if (usrFile != null) {
                Files.deleteIfExists(usrFile);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed delete usr file: {}, {}", usrFile, e);
        }
    }
}
