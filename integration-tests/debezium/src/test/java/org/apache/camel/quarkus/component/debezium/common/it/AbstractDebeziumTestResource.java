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

package org.apache.camel.quarkus.component.debezium.common.it;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Abstract parent for debezium test resources.
 * Parent starts using abstract method.
 */
public abstract class AbstractDebeziumTestResource<T extends GenericContainer<?>>
        implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDebeziumTestResource.class);

    protected T container;
    private Path storeFile;
    private final Type type;

    public AbstractDebeziumTestResource(Type type) {
        this.type = type;
    }

    protected abstract T createContainer();

    protected abstract String getJdbcUrl();

    protected abstract String getUsername();

    protected abstract String getPassword();

    protected abstract int getPort();

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());
        try {
            storeFile = Files.createTempFile(getClass().getSimpleName() + "-store-", "");

            container = createContainer();

            startContainer();

            Map<String, String> map = CollectionHelper.mapOf(
                    type.getPropertyHostname(), container.getHost(),
                    type.getPropertyPort(), container.getMappedPort(getPort()) + "",
                    type.getPropertyOffsetFileName(), storeFile.toString(),
                    type.getPropertyJdbc(), getJdbcUrl());

            if (getUsername() != null) {
                map.put(type.getPropertyUsername(), getUsername());
                map.put(type.getPropertyPassword(), getPassword());
            }

            return map;

        } catch (Exception e) {
            LOGGER.error("Container does not start", e);
            throw new RuntimeException(e);
        }
    }

    protected void startContainer() throws Exception {
        container.start();
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
            if (storeFile != null) {
                Files.deleteIfExists(storeFile);
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
