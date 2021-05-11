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

package org.apache.camel.quarkus.component.cassandraql.it;

import java.util.Map;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

public class CassandraqlTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraqlTestResource.class);
    private static final int PORT = 9042;
    private static final String DOCKER_IMAGE_NAME = "cassandra:3.11.2";

    protected GenericContainer container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());
        try {
            container = new CassandraContainer(DOCKER_IMAGE_NAME)
                    .withExposedPorts(PORT);

            container.start();

            initDB((CassandraContainer) container);

            return CollectionHelper.mapOf(
                    "db.cassandra.url",
                    container.getContainerIpAddress() + ":" + container.getMappedPort(PORT));

        } catch (Exception e) {
            LOGGER.error("Container does not start", e);
            throw new RuntimeException(e);
        }
    }

    private void initDB(CassandraContainer cc) {
        Cluster cluster = cc.getCluster();

        try (Session session = cluster.connect()) {

            session.execute("CREATE KEYSPACE IF NOT EXISTS " + CassandraqlResource.KEYSPACE + " WITH replication = \n" +
                    "{'class':'SimpleStrategy','replication_factor':'1'};");

            session.execute("CREATE TABLE " + CassandraqlResource.KEYSPACE + ".employee(\n" +
                    "   id int PRIMARY KEY,\n" +
                    "   name text,\n" +
                    "   address text\n" +
                    "   );");
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
