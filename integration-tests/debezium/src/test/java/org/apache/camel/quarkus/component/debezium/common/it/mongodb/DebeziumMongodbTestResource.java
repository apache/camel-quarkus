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

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.component.debezium.common.it.Type;
import org.jboss.logging.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class DebeziumMongodbTestResource extends AbstractDebeziumTestResource<GenericContainer> {
    private static final Logger LOG = Logger.getLogger(DebeziumMongodbTestResource.class);

    private static final String PRIVATE_HOST = "mongodb_private";
    private static final String DB_USERNAME = "debezium";
    private static final String DB_PASSWORD = "dbz";
    private static int DB_PORT = 27017;

    public DebeziumMongodbTestResource() {
        super(Type.mongodb);
    }

    private Network net = Network.newNetwork();;

    @Override
    protected GenericContainer createContainer() {
        return new GenericContainer("mongo")
                .withExposedPorts(DB_PORT)
                .withCommand("--replSet", "my-mongo-set")
                .withNetwork(net)
                .withNetworkAliases(PRIVATE_HOST)
                .waitingFor(
                        Wait.forLogMessage(".*Waiting for connections.*", 1));

    }

    @Override
    protected void startContainer() throws Exception {
        super.startContainer();

        execScriptInContainer("initMongodb.txt");
    }

    private void execScriptInContainer(String script) throws Exception {
        String cmd = new String(Files.readAllBytes(Paths.get(getClass().getResource("/" + script).toURI())));
        String[] cmds = cmd.split("\\n\\n");

        for (int i = 0; i < cmds.length; i++) {
            Container.ExecResult er = container.execInContainer(new String[] { "mongo", "--eval", cmds[i] });
        }
    }

    @Override
    protected String getJdbcUrl() {
        final String jdbcUrl = String.format("mongodb://%s:%s@%s:%d", DB_USERNAME, DB_PASSWORD, container.getHost(),
                container.getMappedPort(DB_PORT));

        return jdbcUrl;
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
}
