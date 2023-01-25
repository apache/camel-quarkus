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
package org.apache.camel.quarkus.component.kudu.it;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kudu.KuduConstants;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.CreateTableOptions;
import org.apache.kudu.client.KuduException;
import org.jboss.logging.Logger;

@Path("/kudu")
@ApplicationScoped
public class KuduResource {

    private static final Logger LOG = Logger.getLogger(KuduResource.class);

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/createTable")
    @PUT
    public Response createTable() {
        LOG.info("Calling createTable");

        final List<ColumnSchema> columns = new ArrayList<>(2);
        columns.add(new ColumnSchema.ColumnSchemaBuilder("id", Type.STRING).key(true).build());
        columns.add(new ColumnSchema.ColumnSchemaBuilder("name", Type.STRING).build());

        CreateTableOptions cto = new CreateTableOptions().setRangePartitionColumns(Arrays.asList("id")).setNumReplicas(1);

        final Map<String, Object> headers = new HashMap<>();
        headers.put(KuduConstants.CAMEL_KUDU_SCHEMA, new Schema(columns));
        headers.put(KuduConstants.CAMEL_KUDU_TABLE_OPTIONS, cto);

        producerTemplate.requestBodyAndHeaders("direct:create_table", null, headers);

        return Response.ok().build();
    }

    @Path("/insert")
    @PUT
    public Response insert() {
        LOG.info("Calling insert");

        Map<String, Object> row = new HashMap<>();
        row.put("id", "key1");
        row.put("name", "Samuel");

        producerTemplate.requestBody("direct:insert", row);

        return Response.ok().build();
    }

    @Path("/scan")
    @GET
    public String scan() throws KuduException {
        LOG.info("Calling scan");
        return producerTemplate.requestBody("direct:scan", (Object) null, String.class);
    }

}
