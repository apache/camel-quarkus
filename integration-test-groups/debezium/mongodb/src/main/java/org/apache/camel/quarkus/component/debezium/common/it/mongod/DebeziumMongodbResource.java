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
package org.apache.camel.quarkus.component.debezium.common.it.mongod;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumResource;
import org.apache.camel.quarkus.test.support.debezium.Record;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.eclipse.microprofile.config.Config;

@Path("/debezium-mongodb")
@ApplicationScoped
public class DebeziumMongodbResource extends AbstractDebeziumResource {

    @Inject
    Config config;

    public DebeziumMongodbResource() {
        super(Type.mongodb);
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
        return super.receive();
    }

    @Path("/receiveOperation")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveOperation() {
        Record record = receiveAsRecord();

        if (record == null) {
            return null;
        }
        return record.getOperation();
    }

    @Override
    protected String getEndpointUrl(String hostname, String port, String username, String password, String databaseServerName,
            String offsetStorageFileName) {
        return Type.mongodb.getComponent() + ":localhost?"
                + "offsetStorageFileName=" + offsetStorageFileName
                + "&mongodbUser=" + config.getValue(Type.mongodb.getPropertyUsername(), String.class)
                + "&mongodbPassword=" + config.getValue(Type.mongodb.getPropertyPassword(), String.class)
                + "&mongodbConnectionString=mongodb://" + hostname + ":" + port + "/?replicaSet=my-mongo-set"
                + "&topicPrefix=cq-testing";
    }
}
