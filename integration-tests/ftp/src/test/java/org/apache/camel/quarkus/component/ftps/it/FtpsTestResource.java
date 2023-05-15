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
package org.apache.camel.quarkus.component.ftps.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.apache.camel.quarkus.component.ftp.it.FtpTestResource;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;

public class FtpsTestResource extends FtpTestResource {

    private Path keystoreFilePath;

    public FtpsTestResource() {
        super("ftps");
    }

    @Override
    public Map<String, String> start() {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("server.jks")) {
            Objects.requireNonNull(stream, "FTP keystore file server.jks could not be loaded");
            keystoreFilePath = Files.createTempFile("camel-ftps-keystore", "jks");
            Files.write(keystoreFilePath, stream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return super.start();
    }

    @Override
    protected ListenerFactory createListenerFactory(int port) {
        SslConfigurationFactory sslConfigFactory = new SslConfigurationFactory();
        sslConfigFactory.setKeystoreFile(keystoreFilePath.toFile());
        sslConfigFactory.setKeystoreType("PKCS12");
        sslConfigFactory.setKeystorePassword("password");
        sslConfigFactory.setKeyPassword("password");
        sslConfigFactory.setSslProtocol("TLSv1.3");

        ListenerFactory factory = super.createListenerFactory(port);
        factory.setSslConfiguration(sslConfigFactory.createSslConfiguration());
        return factory;
    }

    @Override
    public void stop() {
        super.stop();
        try {
            Files.deleteIfExists(keystoreFilePath);
        } catch (IOException e) {
            // Ignored
        }
    }
}
