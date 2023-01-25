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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/debezium-sqlserver")
@ApplicationScoped
public class DebeziumSqlserverResource extends AbstractDebeziumResource {

    public static final String PROPERTY_DB_HISTORY_FILE = DebeziumSqlserverResource.class.getSimpleName()
            + "_databaseHistoryFileFilename";

    public static final String DB_NAME = "testDB";

    @ConfigProperty(name = "sqlserver.encrypt", defaultValue = "false")
    Boolean encryptConnection;

    @Inject
    Config config;

    public DebeziumSqlserverResource() {
        super(Type.sqlserver);
    }

    @Path("/receiveAsRecord")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Record receiveAsRecord() {
        return super.receiveAsRecord();
    }

    @Path("/receive")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receive() {
        Record record = super.receiveAsRecord();
        //mssql return empty Strring instead of nulls, wich leads to different status code 200 vs 204
        if (record == null || ("d".equals(record.getOperation()) && "".equals(record.getValue()))) {
            return null;
        }
        return record.getValue();
    }

    @Override
    String getEndpointUrl(String hostname, String port, String username, String password, String databaseServerName,
            String offsetStorageFileName) {
        return super.getEndpointUrl(hostname, port, username, password, databaseServerName, offsetStorageFileName)
                + "&databaseDbname=" + DB_NAME
                + "&databaseHistoryFileFilename=" + config.getValue(PROPERTY_DB_HISTORY_FILE, String.class)
                + "&additionalProperties.database.encrypt=" + encryptConnection;
    }
}
