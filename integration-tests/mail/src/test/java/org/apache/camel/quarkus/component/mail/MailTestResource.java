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
package org.apache.camel.quarkus.component.mail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

public class MailTestResource implements QuarkusTestResourceLifecycleManager {
    private static final String GREENMAIL_CERTIFICATE_STORE_FILE = "greenmail.p12";
    private static final String GENERATE_CERTIFICATE_SCRIPT = "generate-certificates.sh";
    private GenericContainer<?> container;
    private Path certificateStoreLocation;

    @Override
    public Map<String, String> start() {
        try {
            certificateStoreLocation = Files.createTempDirectory("MailTestResource-");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try (InputStream in = classLoader.getResourceAsStream(GREENMAIL_CERTIFICATE_STORE_FILE)) {
                Files.copy(in, certificateStoreLocation.resolve(GREENMAIL_CERTIFICATE_STORE_FILE));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String dockerHost = DockerClientFactory.instance().dockerHostIpAddress();
        if (!dockerHost.equals("localhost") && !dockerHost.equals("127.0.0.1")) {
            regenerateCertificatesForDockerHost();
        }

        //Dockerfile with ImageFromDockerfile is used, because ownership of the certificate has to be changed
        container = new GenericContainer<>(new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder -> {
                    builder.from(ConfigProvider.getConfig().getValue("greenmail.container.image", String.class));
                    builder.copy(GREENMAIL_CERTIFICATE_STORE_FILE, "/home/greenmail/greenmail.p12");
                })
                .withFileFromTransferable(GREENMAIL_CERTIFICATE_STORE_FILE, Transferable.of(getCertificateStoreContent())))
                .withExposedPorts(MailProtocol.allPorts())
                .waitingFor(new HttpWaitStrategy()
                        .forPort(MailProtocol.API.getPort())
                        .forPath("/api/service/readiness")
                        .forStatusCode(200));

        container.start();

        Map<String, String> options = new HashMap<>();
        options.put("mail.host", container.getHost());

        for (MailProtocol protocol : MailProtocol.values()) {
            String optionName = String.format("mail.%s.port", protocol.name().toLowerCase());
            Integer mappedPort = container.getMappedPort(protocol.getPort());
            options.put(optionName, mappedPort.toString());
        }

        return options;
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
        if (certificateStoreLocation != null) {
            try {
                FileUtils.deleteDirectory(certificateStoreLocation.toFile());
            } catch (IOException e) {
                // Ignored
            }
        }
    }

    private void regenerateCertificatesForDockerHost() {
        // Run certificate generation in a container in case the target platform does not have prerequisites like OpenSSL installed (E.g on Windows)
        String imageName = ConfigProvider.getConfig().getValue("eclipse-temurin.container.image", String.class);
        try (GenericContainer<?> container = new GenericContainer<>(imageName)) {
            container.withCreateContainerCmdModifier(modifier -> {
                modifier.withEntrypoint("/bin/bash");
                modifier.withStdinOpen(true);
                modifier.withAttachStdout(true);
            });
            container.setWorkingDirectory("/");
            container.start();

            String host = container.getHost();
            container.copyFileToContainer(
                    MountableFile.forClasspathResource(GENERATE_CERTIFICATE_SCRIPT),
                    "/" + GENERATE_CERTIFICATE_SCRIPT);
            container.execInContainer("/bin/bash", "/" + GENERATE_CERTIFICATE_SCRIPT, host,
                    "DNS:%s,IP:%s".formatted(host, host));
            container.copyFileFromContainer("/" + GREENMAIL_CERTIFICATE_STORE_FILE,
                    certificateStoreLocation.resolve(GREENMAIL_CERTIFICATE_STORE_FILE).toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getCertificateStoreContent() {
        try {
            return Files.readAllBytes(certificateStoreLocation.resolve(GREENMAIL_CERTIFICATE_STORE_FILE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    enum MailProtocol {
        SMTP(3025),
        POP3(3110),
        IMAP(3143),
        SMTPS(3465),
        IMAPS(3993),
        POP3s(3995),
        API(8080);

        private final int port;

        MailProtocol(int port) {
            this.port = port;
        }

        public int getPort() {
            return port;
        }

        public static Integer[] allPorts() {
            MailProtocol[] values = values();
            Integer[] ports = new Integer[values.length];
            for (int i = 0; i < values.length; i++) {
                ports[i] = values[i].getPort();
            }
            return ports;
        }
    }
}
