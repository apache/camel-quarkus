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

package org.apache.camel.quarkus.component.pg.replication.slot.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.apache.camel.quarkus.component.pg.replication.slot.it.PgReplicationSlotRoute.PG_AUTHORITY_CFG_KEY;
import static org.apache.camel.quarkus.component.pg.replication.slot.it.PgReplicationSlotRoute.PG_DBNAME_CFG_KEY;
import static org.apache.camel.quarkus.component.pg.replication.slot.it.PgReplicationSlotRoute.PG_PASSRD_CFG_KEY;
import static org.apache.camel.quarkus.component.pg.replication.slot.it.PgReplicationSlotRoute.PG_USER_CFG_KEY;
import static org.apache.camel.util.CollectionHelper.mapOf;

public class PgReplicationSlotTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(PgReplicationSlotTestResource.class);
    private static final int POSTGRES_PORT = 5432;
    private static final String POSTGRES_IMAGE = "postgres:13.0";
    private static final String POSTGRES_DB_NAME = "camel_db";
    private static final String POSTGRES_PASSWORD = "postgres-password";
    private static final String POSTGRES_USER = "postgres-user";

    private GenericContainer<?> pgContainer;

    @Override
    public Map<String, String> start() {
        LOG.info(TestcontainersConfiguration.getInstance().toString());

        // Setup the Postgres container with replication enabled
        pgContainer = new GenericContainer<>(POSTGRES_IMAGE).withCommand("postgres -c wal_level=logical")
                .withExposedPorts(POSTGRES_PORT).withEnv("POSTGRES_USER", POSTGRES_USER)
                .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD).withEnv("POSTGRES_DB", POSTGRES_DB_NAME)
                .withLogConsumer(new Slf4jLogConsumer(LOG)).waitingFor(Wait.forListeningPort());
        pgContainer.start();

        // Print Postgres server connectivity information
        String pgAuthority = pgContainer.getHost() + ":" + pgContainer.getMappedPort(POSTGRES_PORT);
        LOG.debug("Postgres database available at " + pgAuthority);

        return mapOf(PG_AUTHORITY_CFG_KEY, pgAuthority, PG_DBNAME_CFG_KEY, POSTGRES_DB_NAME, PG_USER_CFG_KEY, POSTGRES_USER,
                PG_PASSRD_CFG_KEY, POSTGRES_PASSWORD);
    }

    @Override
    public void stop() {
        try {
            if (pgContainer != null) {
                pgContainer.stop();
            }
        } catch (Exception ex) {
            LOG.error("An issue occured while stopping the PgReplicationSlotTestResource", ex);
        }
    }
}
