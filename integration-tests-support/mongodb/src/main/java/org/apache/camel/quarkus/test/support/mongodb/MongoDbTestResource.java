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

package org.apache.camel.quarkus.test.support.mongodb;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class MongoDbTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbTestResource.class);

    private static final int MONGODB_PORT = 27017;
    private static final String MONGO_IMAGE = "mongo:4.4";
    private static final String PRIVATE_HOST = "mongodb_private";

    private GenericContainer container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new GenericContainer(MONGO_IMAGE)
                    .withExposedPorts(MONGODB_PORT)
                    .withCommand("--replSet", "my-mongo-set")
                    .withNetwork(Network.newNetwork())
                    .withNetworkAliases(PRIVATE_HOST)
                    .waitingFor(Wait.forListeningPort());

            container.start();

            execScriptInContainer("initMongodb.txt");

            setUpDb();

            String host = container.getContainerIpAddress() + ":" + container.getMappedPort(MONGODB_PORT).toString();

            Map<String, String> config = new HashMap<>();
            config.put("quarkus.mongodb.hosts", host);
            config.put("quarkus.mongodb." + MongoDbConstants.NAMED_MONGO_CLIENT_NAME + ".connection-string",
                    "mongodb://" + host);
            return config;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void setUpDb() {
        final String mongoUrl = "mongodb://" + container.getContainerIpAddress() + ":"
                + container.getMappedPort(MONGODB_PORT).toString();

        MongoClient mongoClient = null;
        try {
            mongoClient = MongoClients.create(mongoUrl);

            MongoDatabase db = mongoClient.getDatabase("test");
            db.createCollection(MongoDbConstants.COLLECTION_TAILING,
                    new CreateCollectionOptions().capped(true).sizeInBytes(1000000000)
                            .maxDocuments(MongoDbConstants.CAP_NUMBER));
            db.createCollection(MongoDbConstants.COLLECTION_PERSISTENT_TAILING,
                    new CreateCollectionOptions().capped(true).sizeInBytes(1000000000)
                            .maxDocuments(MongoDbConstants.CAP_NUMBER));
            db.createCollection(MongoDbConstants.COLLECTION_STREAM_CHANGES);

        } finally {
            if (mongoClient != null) {
                mongoClient.close();
            }
        }
    }

    private void execScriptInContainer(String script) throws Exception {
        String cmd = IOUtils.toString(getClass().getResource("/" + script), StandardCharsets.UTF_8);
        String[] cmds = cmd.split("\\n\\n");

        for (int i = 0; i < cmds.length; i++) {
            Container.ExecResult er = container.execInContainer(new String[] { "mongo", "--eval", cmds[i] });
            if (er.getExitCode() != 0) {
                throw new IllegalStateException("Exec exit code " + er.getExitCode() + ". " + er.getStderr());
            }
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
