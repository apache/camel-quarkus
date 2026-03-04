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
package org.apache.camel.quarkus.component.milvus.it;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.CollectionSchemaParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.highlevel.dml.SearchSimpleParam;
import io.milvus.param.index.CreateIndexParam;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.milvus.MilvusAction;
import org.apache.camel.component.milvus.MilvusHeaders;
import org.jboss.logging.Logger;

@Path("/milvus")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class MilvusResource {

    private static final Logger LOG = Logger.getLogger(MilvusResource.class);

    private static final String COMPONENT_MILVUS = "milvus";

    @Inject
    CamelContext context;

    @Inject
    FluentProducerTemplate fluentTemplate;

    @Inject
    ProducerTemplate producerTemplate;

    @POST
    @Path("/create/{collectionName}")
    public Response createCollection(@PathParam("collectionName") String collectionName,
            Map<String, Object> payload) {

        int dimension = Integer.parseInt(String.valueOf(payload.get("dimension")));
        String vectorField = String.valueOf(payload.get("vector_field"));
        Boolean autoId = Boolean.valueOf(String.valueOf(payload.get("autoID")));

        FieldType field1 = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(autoId)
                .build();

        FieldType field2 = FieldType.newBuilder()
                .withName(vectorField)
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build();

        CollectionSchemaParam schemaParam = CollectionSchemaParam.newBuilder()
                .addFieldType(field1)
                .addFieldType(field2)
                .withEnableDynamicField(true)
                .build();

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withSchema(schemaParam)
                .build();

        Exchange response = fluentTemplate.to("direct:in")
                .withHeader(MilvusHeaders.ACTION, MilvusAction.CREATE_COLLECTION)
                .withHeader(MilvusHeaders.COLLECTION_NAME, collectionName)
                .withBody(createParam)
                .request(Exchange.class);

        if (response.isFailed()) {
            Exception ex = response.getException();
            return Response.status(400).entity(ex.getMessage()).build();
        }
        return Response.ok(response.getMessage().getBody(String.class)).build();

    }

    @POST
    @Path("/index/{collectionName}")
    public Response index(@PathParam("collectionName") String collectionName, Map<String, Object> payload) {
        String vectorField = String.valueOf(payload.get("vector_field"));
        String extraParam = String.valueOf(payload.get("params"));

        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(vectorField)
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.L2)
                .withExtraParam(extraParam)
                .withSyncMode(Boolean.TRUE)
                .build();
        Exchange response = fluentTemplate.to("direct:in")
                .withHeader(MilvusHeaders.ACTION, MilvusAction.CREATE_INDEX)
                .withHeader(MilvusHeaders.COLLECTION_NAME, collectionName)
                .withBody(indexParam)
                .request(Exchange.class);

        if (response.isFailed()) {
            Exception ex = response.getException();
            return Response.status(400).entity(ex.getMessage()).build();
        }
        return Response.ok(response.getMessage().getBody(String.class)).build();

    }

    @POST
    @Path("/insert/{collectionName}")
    public Response insertData(@PathParam("collectionName") String collectionName, List<Map<String, Object>> data) {
        Gson gson = new Gson();
        List<JsonObject> rows = new ArrayList<>();

        for (Map<String, Object> map : data) {
            JsonObject row = gson.toJsonTree(map).getAsJsonObject();
            rows.add(row);
        }

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withRows(rows)
                .build();
        Exchange response = fluentTemplate.to("direct:in")
                .withHeader(MilvusHeaders.ACTION, MilvusAction.INSERT)
                .withHeader(MilvusHeaders.COLLECTION_NAME, collectionName)
                .withBody(insertParam).request(Exchange.class);
        if (response.isFailed() && response.getException() != null) {
            Exception exception = response.getException();
            return Response.status(500)
                    .entity("Milvus Exception: " + exception.getMessage())
                    .build();
        }
        return Response.ok().build();

    }

    @POST
    @Path("/search/{collectionName}")
    public Response search(@PathParam("collectionName") String collectionName, List<Float> payload) {

        SearchSimpleParam searchParam = SearchSimpleParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectors(payload)
                .withLimit(100L)
                .withOffset(0L)
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .build();

        Exchange response = fluentTemplate.to("direct:in")
                .withHeader(MilvusHeaders.ACTION, MilvusAction.SEARCH)
                .withHeader(MilvusHeaders.COLLECTION_NAME, collectionName)
                .withBody(searchParam)
                .request(Exchange.class);
        if (response.isFailed()) {
            Exception ex = response.getException();
            return Response.status(400).entity(ex.getMessage()).build();
        }
        return Response.ok(response.getMessage().getBody(String.class)).build();

    }

    @POST
    @Path("/delete/{collectionName}")
    public Response delete(@PathParam("collectionName") String collectionName, List<Long> ids) {

        String expr = "id in [" + ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";

        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();

        Exchange response = fluentTemplate.to("direct:in")
                .withHeader(MilvusHeaders.ACTION, MilvusAction.DELETE)
                .withHeader(MilvusHeaders.COLLECTION_NAME, collectionName)
                .withBody(deleteParam)
                .request(Exchange.class);

        if (response.isFailed()) {
            Exception ex = response.getException();
            return Response.status(400).entity(ex.getMessage()).build();
        }
        return Response.ok(response.getMessage().getBody(String.class)).build();

    }

}
