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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kudu.KuduConstants;
import org.apache.camel.util.ObjectHelper;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.CreateTableOptions;
import org.apache.kudu.client.KuduPredicate;

@Path("/kudu")
@ApplicationScoped
public class KuduResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/createTable")
    @PUT
    public Response createTable() {
        final List<ColumnSchema> columns = new ArrayList<>(2);
        columns.add(new ColumnSchema.ColumnSchemaBuilder("id", Type.STRING).key(true).build());
        columns.add(new ColumnSchema.ColumnSchemaBuilder("name", Type.STRING).build());
        columns.add(new ColumnSchema.ColumnSchemaBuilder("age", Type.INT32).build());

        CreateTableOptions cto = new CreateTableOptions()
                .setRangePartitionColumns(List.of("id"))
                .setNumReplicas(1);

        final Map<String, Object> headers = new HashMap<>();
        headers.put(KuduConstants.CAMEL_KUDU_SCHEMA, new Schema(columns));
        headers.put(KuduConstants.CAMEL_KUDU_TABLE_OPTIONS, cto);

        producerTemplate.requestBodyAndHeaders("direct:create_table", null, headers);

        return Response.ok().build();
    }

    @Path("/insert")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert(Map<String, Object> insertRowData) {
        producerTemplate.requestBody("direct:insert", insertRowData);

        return Response.ok().build();
    }

    @Path("/update")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(Map<String, Object> updateRowData) {
        producerTemplate.requestBody("direct:update", updateRowData);

        return Response.ok().build();
    }

    @Path("/delete/{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);

        producerTemplate.requestBody("direct:delete", row);

        return Response.ok().build();
    }

    @Path("/upsert")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upsertPatch(Map<String, Object> upsertRowData) {
        producerTemplate.requestBody("direct:upsert", upsertRowData);

        return Response.ok().build();
    }

    @Path("/upsert")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upsertPost(Map<String, Object> upsertRowData) {
        producerTemplate.requestBody("direct:upsert", upsertRowData);

        return Response.ok().build();
    }

    @SuppressWarnings("unchecked")
    @Path("/scan")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> scan(@QueryParam("columnNames") String columnNames) {
        Map<String, Object> headers = new HashMap<>();
        if (ObjectHelper.isNotEmpty(columnNames)) {
            headers.put(KuduConstants.CAMEL_KUDU_SCAN_COLUMN_NAMES, Arrays.asList(columnNames.split(",")));
        }
        return producerTemplate.requestBodyAndHeaders("direct:scan", null, headers, List.class);
    }

    @SuppressWarnings("unchecked")
    @Path("/scan/predicate")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> scanWithAgePredicate(@QueryParam("minAge") int minAge) {
        Map<String, Object> headers = new HashMap<>();
        ColumnSchema ageColumn = new ColumnSchema.ColumnSchemaBuilder("age", Type.INT32).build();
        KuduPredicate predicate = KuduPredicate.newComparisonPredicate(ageColumn, KuduPredicate.ComparisonOp.GREATER_EQUAL,
                minAge);
        headers.put(KuduConstants.CAMEL_KUDU_SCAN_PREDICATE, predicate);
        return producerTemplate.requestBodyAndHeaders("direct:scan", null, headers, List.class);
    }

    @SuppressWarnings("unchecked")
    @Path("/scan/limit")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> scanWithLimit(@QueryParam("limit") long limit) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KuduConstants.CAMEL_KUDU_SCAN_LIMIT, limit);
        return producerTemplate.requestBodyAndHeaders("direct:scan", null, headers, List.class);
    }
}
