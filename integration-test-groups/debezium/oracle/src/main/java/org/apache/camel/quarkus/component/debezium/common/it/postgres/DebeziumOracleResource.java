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

import io.debezium.storage.file.history.FileSchemaHistory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumResource;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.eclipse.microprofile.config.Config;

@Path("/debezium-oracle")
@ApplicationScoped
public class DebeziumOracleResource extends AbstractDebeziumResource {

    public static final String PROPERTY_DB_HISTORY_FILE = DebeziumOracleResource.class.getSimpleName()
            + "_databaseHistoryFileFilename";

    @Inject
    Config config;

    public static final String DB_NAME = "oracle";

    public DebeziumOracleResource() {
        super(Type.oracle);
    }

    @Path("/receive")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receive() {
        return super.receive();
    }

    @Override
    protected String getEndpointUrl(String hostname, String port, String username, String password, String databaseServerName,
            String offsetStorageFileName) {
        //we have oracle-xe - multitenant, so we need to configure pdbname and use CDB (FREE in xe) as dbName
        return super.getEndpointUrl(hostname, port, "c##dbzuser", "dbz", databaseServerName, offsetStorageFileName)
                + "&databaseDbname=FREE"
                + "&databasePdbName=" + DB_NAME
                + "&schemaHistoryInternal=" + FileSchemaHistory.class.getName()
                + "&schemaHistoryInternalFileFilename=" + config.getValue(PROPERTY_DB_HISTORY_FILE, String.class);
    }

}
