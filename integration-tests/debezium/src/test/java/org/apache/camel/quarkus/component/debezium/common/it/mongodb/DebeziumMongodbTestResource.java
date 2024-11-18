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

package org.apache.camel.quarkus.component.debezium.common.it.mongodb;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.component.debezium.common.it.Type;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.apache.camel.quarkus.test.support.mongodb.MongoDbTestSupportUtils.getMongoScriptExecutable;

public class DebeziumMongodbTestResource extends AbstractDebeziumTestResource<GenericContainer<?>> {
    private static final Logger LOG = Logger.getLogger(AbstractDebeziumTestResource.class);
    private static final String PRIVATE_HOST = "mongodb_private";
    private static final String DB_USERNAME = "debezium";
    private static final String DB_PASSWORD = "dbz";
    private static final String DB_INIT_SCRIPT = "initMongodb.txt";
    private static final int DB_PORT = AvailablePortFinder.getNextAvailable();
    private static final String MONGO_IMAGE_NAME = ConfigProvider.getConfig().getValue("mongodb.container.image", String.class);

    public DebeziumMongodbTestResource() {
        super(Type.mongodb);
    }

    private final Network net = Network.newNetwork();;

    @Override
    protected GenericContainer<?> createContainer() {
        return new FixedHostPortGenericContainer<>(MONGO_IMAGE_NAME)
                .withFixedExposedPort(DB_PORT, DB_PORT)
                .withCommand("--replSet", "my-mongo-set", "--port", String.valueOf(DB_PORT), "--bind_ip",
                        "localhost," + PRIVATE_HOST)
                .withNetwork(net)
                .withNetworkAliases(PRIVATE_HOST)
                .waitingFor(
                        Wait.forLogMessage(".*Waiting for connections.*", 1));
    }

    @Override
    protected void startContainer() throws Exception {
        super.startContainer();
        execScriptInContainer();
    }

    @Override
    public void stop() {
        super.stop();
        AvailablePortFinder.releaseReservedPorts();
    }

    private void execScriptInContainer() throws Exception {
        URL resource = getClass().getResource("/" + DB_INIT_SCRIPT);
        Objects.requireNonNull(resource, DB_INIT_SCRIPT + " could not be found");

        String script = IOUtils.toString(resource, StandardCharsets.UTF_8);
        script = script.replace("%container-host%", getHostPort());
        for (String cmd : script.split("\\n\\n")) {
            Container.ExecResult er = container.execInContainer(getMongoScriptExecutable(MONGO_IMAGE_NAME), "--port",
                    String.valueOf(DB_PORT), "--eval", cmd);
            if (er.getExitCode() != 0) {
                LOG.errorf("Error executing MongoDB command: %s", cmd);
                LOG.error(er.getStdout());
                LOG.error(er.getStderr());
                throw new RuntimeException("Error starting mongodb container");
            }
        }
    }

    @Override
    protected String getJdbcUrl() {
        return String.format("mongodb://%s:%s@%s", DB_USERNAME, DB_PASSWORD, getHostPort());
    }

    @Override
    protected String getUsername() {
        return DB_USERNAME;
    }

    @Override
    protected String getPassword() {
        return DB_PASSWORD;
    }

    @Override
    protected int getPort() {
        return DB_PORT;
    }

    private String getHostPort() {
        return String.format("%s:%d", container.getHost(), container.getMappedPort(DB_PORT));
    }
}
