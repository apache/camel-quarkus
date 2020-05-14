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
package org.apache.camel.quarkus.component.debezium.postgres.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ConsumerTemplate;

@Path("/debezium-postgres")
@ApplicationScoped
public class DebeziumPostgresResource {

    public static final String DB_NAME = "postgresDB";
    public static final String DB_USERNAME = "user";
    public static final String DB_PASSWORD = "changeit";

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/receive")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receive(@QueryParam("hostname") String hostname,
            @QueryParam("port") int port,
            @QueryParam("offsetStorageFileName") String offsetStorageFileName)
            throws Exception {
        return consumerTemplate.receiveBody("debezium-postgres:localhost?"
                + "databaseHostname=" + hostname
                + "&databasePort=" + port
                + "&databaseUser=" + DebeziumPostgresResource.DB_USERNAME
                + "&databasePassword=" + DebeziumPostgresResource.DB_PASSWORD
                + "&databaseDbname=" + DebeziumPostgresResource.DB_NAME
                + "&databaseServerName=qa"
                + "&offsetStorageFileName=" + offsetStorageFileName, 2000, String.class);
    }

}
