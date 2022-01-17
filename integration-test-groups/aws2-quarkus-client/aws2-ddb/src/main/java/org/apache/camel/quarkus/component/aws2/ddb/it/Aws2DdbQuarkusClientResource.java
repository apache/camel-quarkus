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
package org.apache.camel.quarkus.component.aws2.ddb.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.component.aws2.ddb.Ddb2Endpoint;
import org.apache.camel.component.aws2.ddb.Ddb2Operations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Path("/aws2-ddb-quarkus-client")
@ApplicationScoped
public class Aws2DdbQuarkusClientResource {

    @Inject
    CamelContext context;

    @Inject
    DynamoDbClient dynamoDB;

    @ConfigProperty(name = "aws-ddb.table-name")
    String tableName;

    @Path("/verify/client")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean quarkusManagesDynamoDbClient() {
        Ddb2Endpoint endpoint = context.getEndpoint(componentUri(Ddb2Operations.GetItem), Ddb2Endpoint.class);
        DynamoDbClient camelDynamoDbClient = endpoint.getConfiguration().getAmazonDDBClient();
        return camelDynamoDbClient != null && camelDynamoDbClient.equals(dynamoDB);
    }

    private String componentUri(Ddb2Operations op) {
        return componentUri(Aws2DdbResource.Table.basic, op);
    }

    private String componentUri(Aws2DdbResource.Table table, Ddb2Operations op) {
        return "aws2-ddb://" + this.tableName + "?operation=" + op;
    }
}
