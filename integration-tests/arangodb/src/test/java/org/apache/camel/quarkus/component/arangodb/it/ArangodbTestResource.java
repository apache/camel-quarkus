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
package org.apache.camel.quarkus.component.arangodb.it;

import java.util.Map;

import com.github.dockerjava.api.model.Ulimit;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class ArangodbTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArangodbTestResource.class);
    private GenericContainer<?> container;
    private static final String CONTAINER_NAME = "arango";
    private static final String ARANGO_IMAGE = ConfigProvider.getConfig().getValue("arangodb.container.image", String.class);
    private static final String ARANGO_NO_AUTH = "ARANGO_NO_AUTH";
    private static final Integer PORT_DEFAULT = 8529;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new GenericContainer<>(ARANGO_IMAGE)
                    .withExposedPorts(PORT_DEFAULT)
                    .withEnv(ARANGO_NO_AUTH, "1")
                    .withNetworkAliases(CONTAINER_NAME)
                    .waitingFor(Wait.forLogMessage(".*ArangoDB [(]version .*[)] is ready for business. Have fun!.*", 1))
                    .withCreateContainerCmdModifier(
                            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[] { new Ulimit("nofile", 65535L, 65535L) }));

            container.start();

            return CollectionHelper.mapOf(
                    "camel.arangodb.port",
                    container.getMappedPort(PORT_DEFAULT).toString(),
                    "camel.arangodb.host",
                    container.getHost());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
