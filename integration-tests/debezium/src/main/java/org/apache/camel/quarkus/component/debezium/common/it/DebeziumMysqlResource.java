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

@Path("/debezium-mysql")
@ApplicationScoped
public class DebeziumMysqlResource extends AbstractDebeziumResource {

    public static final String PROPERTY_DB_HISTORY_FILE = DebeziumMysqlResource.class.getSimpleName()
            + "_databaseHistoryFileFilename";

    //debezium on mysql needs more privileges, therefore it will use root user
    public static final String DB_ROOT_USERNAME = "root";

    @Inject
    Config config;

    public DebeziumMysqlResource() {
        super(Type.mysql);
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
        String s = super.receive();
        return s;
    }

    @Override
    String getEndpointUrl(String hostname, String port, String username, String password, String databaseServerName,
            String offsetStorageFileName) {
        //use root user to get all required privileges
        return super.getEndpointUrl(hostname, port, DB_ROOT_USERNAME, password, databaseServerName, offsetStorageFileName)
                //and add specific parameters
                + "&databaseServerId=223344"
                + "&databaseHistoryFileFilename=" + config.getValue(PROPERTY_DB_HISTORY_FILE, String.class);
    }
}
