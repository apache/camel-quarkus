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
package org.apache.camel.quarkus.component.debezium.common.it.postgres;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumResource;
import org.apache.camel.quarkus.test.support.debezium.Type;

@Path("/debezium-postgres")
@ApplicationScoped
public class DebeziumPostgresResource extends AbstractDebeziumResource {

    public static final String DB_NAME = "PostgresDB";

    public DebeziumPostgresResource() {
        super(Type.postgres);
    }

    @Path("/receiveEmptyMessages")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveEmptyMessages() {
        return super.receiveEmptyMessages();
    }

    @Path("/receive")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receive() {
        return super.receive();
    }

    @Override
    protected String getKafkaOffsetEndpointUrl() {
        String kafkaBootstrapServers = config.getOptionalValue("kafka.bootstrap.servers", String.class).orElse(null);
        if (kafkaBootstrapServers == null) {
            return null;
        }
        String hostname = config.getValue(Type.postgres.getPropertyHostname(), String.class);
        String port = config.getValue(Type.postgres.getPropertyPort(), String.class);
        String username = config.getValue(Type.postgres.getPropertyUsername(), String.class);
        String password = config.getValue(Type.postgres.getPropertyPassword(), String.class);
        return Type.postgres.getComponent() + ":localhost?"
                + "databaseHostname=" + hostname
                + "&databasePort=" + port
                + "&databaseUser=" + username
                + "&databasePassword=" + password
                + "&databaseDbname=" + DB_NAME
                + "&topicPrefix=cq-testing-kafka"
                + "&slotName=debezium_kafka"
                + "&offsetStorage=org.apache.kafka.connect.storage.KafkaOffsetBackingStore"
                + "&offsetStorageTopic=debezium-offset-storage-postgres"
                + "&offsetStoragePartitions=1"
                + "&offsetStorageReplicationFactor=1"
                + "&offsetFlushIntervalMs=1000"
                + "&additionalProperties.bootstrap.servers=" + kafkaBootstrapServers;
    }

    @Override
    protected String getEndpointUrl(String hostname, String port, String username, String password,
            String databaseServerName, String offsetStorageFileName) {
        return super.getEndpointUrl(hostname, port, username, password, databaseServerName, offsetStorageFileName)
                + "&databaseDbname=" + DB_NAME;
    }
}
