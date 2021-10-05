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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.SdkClient;

public final class Aws2TestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(Aws2TestResource.class);

    private Aws2TestEnvContext envContext;

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {
        final String realKey = System.getenv("AWS_ACCESS_KEY");
        final String realSecret = System.getenv("AWS_SECRET_KEY");
        final String realRegion = System.getenv("AWS_REGION");
        final boolean realCredentialsProvided = realKey != null && realSecret != null && realRegion != null;
        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
        final boolean usingMockBackend = startMockBackend && !realCredentialsProvided;

        ServiceLoader<Aws2TestEnvCustomizer> loader = ServiceLoader.load(Aws2TestEnvCustomizer.class);
        List<Aws2TestEnvCustomizer> customizers = new ArrayList<>();
        for (Aws2TestEnvCustomizer customizer : loader) {
            LOG.info("Loaded Aws2TestEnvCustomizer " + customizer.getClass().getName());
            customizers.add(customizer);
        }
        LOG.info("Loaded " + customizers.size() + " Aws2TestEnvCustomizers");
        if (usingMockBackend) {
            MockBackendUtils.logMockBackendUsed();

            final Service[] services = customizers.stream()
                    .map(Aws2TestEnvCustomizer::localstackServices)
                    .flatMap((Service[] ss) -> Stream.of(ss))
                    .distinct()
                    .toArray(Service[]::new);

            final Service[] exportCredentialsServices = customizers.stream()
                    .map(Aws2TestEnvCustomizer::exportCredentialsForLocalstackServices)
                    .flatMap((Service[] ss) -> Stream.of(ss))
                    .distinct()
                    .toArray(Service[]::new);

            LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.17.5"))
                    .withServices(services);
            localstack.withEnv("LAMBDA_EXECUTOR", "local");
            localstack.withLogConsumer(new Slf4jLogConsumer(LOG));
            localstack.start();

            envContext = new Aws2TestEnvContext(localstack.getAccessKey(), localstack.getSecretKey(), localstack.getRegion(),
                    Optional.of(localstack), exportCredentialsServices);

        } else {
            if (!startMockBackend && !realCredentialsProvided) {
                throw new IllegalStateException(
                        "Set AWS_ACCESS_KEY, AWS_SECRET_KEY and AWS_REGION env vars if you set CAMEL_QUARKUS_START_MOCK_BACKEND=false");
            }
            MockBackendUtils.logRealBackendUsed();
            envContext = new Aws2TestEnvContext(realKey, realSecret, realRegion, Optional.empty(), new Service[0]);
        }

        customizers.forEach(customizer -> customizer.customize(envContext));

        return envContext.getProperies();
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
            }
            c = c.getSuperclass();
        }

    }

}
