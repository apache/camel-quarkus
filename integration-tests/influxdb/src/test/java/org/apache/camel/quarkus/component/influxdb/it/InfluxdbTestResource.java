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

package org.apache.camel.quarkus.component.influxdb.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class InfluxdbTestResource implements QuarkusTestResourceLifecycleManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(InfluxdbTestResource.class);
    public static final int INFLUXDB_PORT = 8086;
    public static final String INFLUXDB_VERSION = "1.7.10";
    public static final String INFLUXDB_IMAGE = "influxdb:" + INFLUXDB_VERSION;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new GenericContainer<>(INFLUXDB_IMAGE)
                    .withExposedPorts(INFLUXDB_PORT)
                    .waitingFor(Wait.forListeningPort());

            container.start();

            return CollectionHelper.mapOf(
                    InfluxdbResource.INFLUXDB_CONNECTION_PROPERTY,
                    "http://" + String.format("%s:%s", container.getHost(),
                            container.getMappedPort(INFLUXDB_PORT)));
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
