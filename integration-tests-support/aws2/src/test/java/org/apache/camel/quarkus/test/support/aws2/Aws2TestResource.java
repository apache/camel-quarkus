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
package org.apache.camel.quarkus.test.support.aws2;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.SdkClient;

public final class Aws2TestResource implements QuarkusTestResourceLifecycleManager {
    static final int FLOCI_PORT = 4566;
    private static final Logger LOG = LoggerFactory.getLogger(Aws2TestResource.class);

    private Aws2TestEnvContext envContext;

    @Override
    public Map<String, String> start() {
        final String realKey = System.getenv("AWS_ACCESS_KEY");
        final String realSecret = System.getenv("AWS_SECRET_KEY");
        final String realRegion = System.getenv("AWS_REGION");
        final String defaultCredentialsProviderValue = System.getenv("AWS_USE_DEFAULT_CREDENTIALS_PROVIDER");

        final boolean realCredentialsProvided = realKey != null && realSecret != null && realRegion != null;
        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
        final boolean useDefaultCredentialsProvider = defaultCredentialsProviderValue != null &&
                !defaultCredentialsProviderValue.isEmpty() &&
                Boolean.parseBoolean(defaultCredentialsProviderValue);
        final boolean usingMockBackend = startMockBackend && !realCredentialsProvided && !useDefaultCredentialsProvider;

        ServiceLoader<Aws2TestEnvCustomizer> loader = ServiceLoader.load(Aws2TestEnvCustomizer.class);
        List<Aws2TestEnvCustomizer> customizers = new ArrayList<>();
        for (Aws2TestEnvCustomizer customizer : loader) {
            LOG.info("Loaded Aws2TestEnvCustomizer {}", customizer.getClass().getName());
            customizers.add(customizer);
        }
        LOG.info("Loaded {} Aws2TestEnvCustomizers", customizers.size());
        if (usingMockBackend) {
            MockBackendUtils.logMockBackendUsed();

            final Service[] exportCredentialsServices = customizers.stream()
                    .map(Aws2TestEnvCustomizer::exportCredentialsForMockServices)
                    .flatMap(Stream::of)
                    .distinct()
                    .toArray(Service[]::new);

            boolean needsDockerSocket = customizers.stream()
                    .map(Aws2TestEnvCustomizer::awsServices)
                    .flatMap(Stream::of)
                    .anyMatch(s -> s == Service.LAMBDA);

            DockerImageName imageName = DockerImageName
                    .parse(ConfigProvider.getConfig().getValue("floci.container.image", String.class));

            GenericContainer<?> floci = new GenericContainer<>(imageName)
                    .withExposedPorts(FLOCI_PORT)
                    .waitingFor(Wait.forHttp("/_floci/health").forPort(FLOCI_PORT))
                    .withEnv("AWS_ACCESS_KEY_ID", "testAccessKeyId") //has to be longer then `test`, to work on FIPS systems
                    .withEnv("AWS_SECRET_ACCESS_KEY", "testSecretKeyId")
                    .withLogConsumer(new Slf4jLogConsumer(LOG));

            if (needsDockerSocket) {
                Path dockerSocket = Path.of("/var/run/docker.sock");
                if (Files.exists(dockerSocket)) {
                    floci.withFileSystemBind(dockerSocket.toString(), "/var/run/docker.sock", BindMode.READ_WRITE)
                            .withPrivilegedMode(true);
                } else {
                    LOG.warn("Docker socket not found at {}. Lambda container execution will not be available.",
                            dockerSocket);
                }
            }

            String logLevel = System.getProperty("floci.log.level", System.getenv("FLOCI_LOG_LEVEL"));
            if (logLevel != null) {
                floci.withEnv("QUARKUS_LOG_LEVEL", logLevel);
            }

            floci.start();

            envContext = new Aws2TestEnvContext(getAccessKey(), getSecretKey(), getRegion(),
                    useDefaultCredentialsProvider, Optional.of(floci), exportCredentialsServices);

        } else {
            if (!startMockBackend && !realCredentialsProvided && !useDefaultCredentialsProvider) {
                throw new IllegalStateException(
                        "Set AWS_ACCESS_KEY, AWS_SECRET_KEY and AWS_REGION env vars or AWS_USE_DEFAULT-CREDENTIALS-PROVIDER to true if you set CAMEL_QUARKUS_START_MOCK_BACKEND=false");
            }
            if (realCredentialsProvided && useDefaultCredentialsProvider) {
                throw new IllegalStateException(
                        "Real account (AWS_ACCESS_KEY and AWS_SECRET_KEY) can not be used together with default credentials provider (AWS_USE_DEFAULT_CREDENTIALS_PROVIDER = true)");
            }
            MockBackendUtils.logRealBackendUsed();
            envContext = new Aws2TestEnvContext(realKey, realSecret, realRegion, useDefaultCredentialsProvider,
                    Optional.empty(), new Service[0]);
        }

        customizers.forEach(customizer -> customizer.customize(envContext));

        return envContext.getProperties();
    }

    @Override
    public void stop() {
        envContext.close();
    }

    @Override
    public void inject(Object testInstance) {
        Class<?> c = testInstance.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                Aws2Client clientAnnot = f.getAnnotation(Aws2Client.class);
                if (clientAnnot != null) {
                    Service service = clientAnnot.value();
                    f.setAccessible(true);
                    SdkClient client = envContext.client(service, f.getType());
                    try {
                        f.set(testInstance, client);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new RuntimeException("Could not set " + c.getName() + "." + f.getName(), e);
                    }
                }
                Aws2MockBackend mockBackendAnnot = f.getAnnotation(Aws2MockBackend.class);
                if (mockBackendAnnot != null) {
                    f.setAccessible(true);
                    try {
                        f.set(testInstance, envContext.isMockBackend());
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new RuntimeException("Could not set " + c.getName() + "." + f.getName(), e);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    private String getAccessKey() {
        return "testAccessKeyId";
    }

    private String getSecretKey() {
        return "testSecretKeyId";
    }

    private String getRegion() {
        return "us-east-1";
    }

}
