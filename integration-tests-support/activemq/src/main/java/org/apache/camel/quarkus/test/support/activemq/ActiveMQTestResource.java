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
package org.apache.camel.quarkus.test.support.activemq;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.dockerjava.api.model.Ulimit;
import io.quarkus.artemis.core.runtime.ArtemisBuildTimeConfig;
import io.quarkus.artemis.core.runtime.ArtemisBuildTimeConfigs;
import io.quarkus.artemis.core.runtime.ArtemisDevServicesBuildTimeConfig;
import io.quarkus.artemis.core.runtime.ArtemisUtil;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class ActiveMQTestResource implements QuarkusTestResourceLifecycleManager {
    private static final String ACTIVEMQ_USERNAME = "artemis";
    private static final String ACTIVEMQ_PASSWORD = "simetraehcapa";
    private static final int ACTIVEMQ_PORT = 61616;

    private GenericContainer<?> container;
    private String[] modules;
    private String[] javaArgs = new String[] {};

    @Override
    public void init(Map<String, String> initArgs) {
        initArgs.forEach((name, value) -> {
            if (name.equals("modules")) {
                modules = value.split(",");
            }
            if (name.equals("java-args")) {
                javaArgs = value.split(",");
            }
        });
    }

    @Override
    public Map<String, String> start() {
        DockerImageName imageName = DockerImageName.parse(getArtemisImageName());
        container = new GenericContainer<>(imageName)
                .withExposedPorts(ACTIVEMQ_PORT)
                .withLogConsumer(frame -> System.out.print(frame.getUtf8String()))
                .withEnv("AMQ_USER", ACTIVEMQ_USERNAME)
                .withEnv("AMQ_PASSWORD", ACTIVEMQ_PASSWORD)
                .withEnv("JAVA_ARGS_APPEND", "-Dbrokerconfig.maxDiskUsage=-1 " + String.join(" ", javaArgs))
                .waitingFor(Wait.forLogMessage(".*AMQ241001.*", 1))
                .withCreateContainerCmdModifier(
                        cmd -> cmd.getHostConfig().withUlimits(new Ulimit[] { new Ulimit("nofile", 2048L, 2048L) }));

        container.start();

        int containerPort = container.getMappedPort(ACTIVEMQ_PORT);
        String containerHost = container.getHost();

        String brokerUrlTcp = String.format("tcp://%s:%d", containerHost, containerPort);
        String brokerUrlWs = String.format("ws://%s:%d", containerHost, containerPort);
        String brokerUrlAmqp = String.format("amqp://%s:%d", containerHost, containerPort);

        Map<String, String> result = new LinkedHashMap<>();

        if (modules != null) {
            Arrays.stream(modules).forEach(module -> {
                if (module.equals("quarkus.artemis")) {
                    result.put("quarkus.artemis.url", brokerUrlTcp);
                    result.put("quarkus.artemis.username", ACTIVEMQ_USERNAME);
                    result.put("quarkus.artemis.password", ACTIVEMQ_PASSWORD);
                } else if (module.equals("quarkus.qpid-jms")) {
                    result.put("quarkus.qpid-jms.url", brokerUrlAmqp);
                    result.put("quarkus.qpid-jms.username", ACTIVEMQ_USERNAME);
                    result.put("quarkus.qpid-jms.password", ACTIVEMQ_PASSWORD);
                } else if (module.startsWith("camel.component")) {
                    result.put(module + ".brokerUrl", brokerUrlTcp);
                    result.put(module + ".username", ACTIVEMQ_USERNAME);
                    result.put(module + ".password", ACTIVEMQ_PASSWORD);
                } else if (module.equals("broker-url.ws")) {
                    result.put("broker-url.ws", brokerUrlWs);
                }
            });
        }

        return result;
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    private String getArtemisImageName() {
        // Align to the same image used by quarkus-artemis
        return new SmallRyeConfigBuilder()
                .addSystemSources()
                .withValidateUnknown(false)
                .withMapping(ArtemisBuildTimeConfig.class)
                .withMapping(ArtemisBuildTimeConfigs.class)
                .withMapping(ArtemisDevServicesBuildTimeConfig.class)
                .build()
                .getConfigMapping(ArtemisBuildTimeConfigs.class).configs().get(ArtemisUtil.DEFAULT_CONFIG_NAME)
                .devservices().getImageName();
    }
}
