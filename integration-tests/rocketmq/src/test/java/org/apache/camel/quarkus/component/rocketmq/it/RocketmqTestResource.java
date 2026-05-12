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
package org.apache.camel.quarkus.component.rocketmq.it;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

public class RocketmqTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(RocketmqTestResource.class);
    private static final String ROCKETMQ_IMAGE = ConfigProvider.getConfig()
            .getValue("rocketmq.container.image", String.class);
    private static final int NAMESRV_PORT = 9876;

    private GenericContainer<?> namesrvContainer;
    private GenericContainer<?> brokerContainer;
    private Network network;

    @Override
    public Map<String, String> start() {
        network = Network.newNetwork();

        namesrvContainer = new GenericContainer<>(ROCKETMQ_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("namesrv")
                .withExposedPorts(NAMESRV_PORT)
                .withCommand("sh", "mqnamesrv")
                .waitingFor(Wait.forLogMessage(".*The Name Server boot success.*", 1));

        namesrvContainer.start();

        int brokerPort = AvailablePortFinder.getNextAvailable();
        String brokerConf = String.format(
                "brokerIP1=%s%nlistenPort=%d%nbrokerName=broker-a%nbrokerClusterName=DefaultCluster%n",
                namesrvContainer.getHost(), brokerPort);

        brokerContainer = new RocketMQBrokerContainer(ROCKETMQ_IMAGE, brokerPort)
                .withNetwork(network)
                .withNetworkAliases("broker")
                .withCopyToContainer(Transferable.of(brokerConf.getBytes(StandardCharsets.UTF_8)), "/tmp/broker.conf")
                .withCommand("sh", "mqbroker", "-n", "namesrv:9876", "-c", "/tmp/broker.conf")
                .dependsOn(namesrvContainer)
                .waitingFor(Wait.forLogMessage(".*The broker\\[.*\\] boot success.*", 1));

        brokerContainer.start();

        createTopic();

        Map<String, String> properties = new HashMap<>();
        properties.put("camel.component.rocketmq.namesrv-addr",
                namesrvContainer.getHost() + ":" + namesrvContainer.getMappedPort(NAMESRV_PORT));
        LOG.info("RocketMQ test properties: {}", properties);
        return properties;
    }

    private void createTopic() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        org.testcontainers.containers.Container.ExecResult lastResult = null;

        while (System.nanoTime() < deadline) {
            lastResult = updateTopic();
            if (lastResult.getExitCode() == 0 && lastResult.getStdout().contains("success")) {
                LOG.info("Topic creation stdout: {}", lastResult.getStdout());
                return;
            }

            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while creating RocketMQ topic", e);
            }
        }

        throw new IllegalStateException(String.format(
                "Could not create RocketMQ topic. Exit code: %d, stdout: %s, stderr: %s",
                lastResult.getExitCode(), lastResult.getStdout(), lastResult.getStderr()));
    }

    private org.testcontainers.containers.Container.ExecResult updateTopic() {
        try {
            return brokerContainer.execInContainer(
                    "sh", "mqadmin", "updateTopic", "-n", "namesrv:9876", "-t", "camel-test", "-c", "DefaultCluster");
        } catch (Exception e) {
            throw new IllegalStateException("Could not create topic via mqadmin", e);
        }
    }

    @Override
    public void stop() {
        try {
            if (brokerContainer != null) {
                brokerContainer.stop();
            }
        } catch (Exception e) {
            LOG.error("Error stopping broker container", e);
        }
        try {
            if (namesrvContainer != null) {
                namesrvContainer.stop();
            }
        } catch (Exception e) {
            LOG.error("Error stopping nameserver container", e);
        }
        try {
            if (network != null) {
                network.close();
            }
        } catch (Exception e) {
            LOG.error("Error closing RocketMQ test network", e);
        }
    }

    static class RocketMQBrokerContainer extends GenericContainer<RocketMQBrokerContainer> {
        RocketMQBrokerContainer(String image, int brokerPort) {
            super(image);
            addFixedExposedPort(brokerPort, brokerPort);
        }
    }
}
