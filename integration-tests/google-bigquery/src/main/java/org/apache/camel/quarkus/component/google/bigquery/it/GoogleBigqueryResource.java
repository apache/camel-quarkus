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
package org.apache.camel.quarkus.component.google.bigquery.it;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/google-bigquery")
public class GoogleBigqueryResource {

    public static final String DATASET_ID = "cq_testing";
    public static final String TABLE_NAME = "camel_quarkus_basic";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    @ConfigProperty(name = "google.project.id", defaultValue = "test")
    String projectId;

    String tableId = DATASET_ID + "." + TABLE_NAME;

    @Path("/table")
    @POST
    public Response createTable() {
        String sql = "CREATE TABLE `" + tableId + "` (id NUMERIC, col1 STRING, col2 STRING)";
        producerTemplate.requestBody("google-bigquery-sql:" + projectId + ":" + sql, null,
                Long.class);
        return Response.created(URI.create("https://camel.apache.org")).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertRow(Map<String, String> tableData) {
        producerTemplate.requestBody("google-bigquery:" + projectId + ":" + DATASET_ID + ":" + TABLE_NAME, tableData);
        return Response.created(URI.create("https://camel.apache.org")).build();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRow() {
        String sql = "SELECT * FROM `" + tableId + "`";
        Long rowCount = producerTemplate.requestBody("google-bigquery-sql:" + projectId + ":" + sql, null, Long.class);
        return Response.ok(rowCount).build();
    }

    @Path("/file")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRowUsingQueryResource() throws IOException {
        String sql = "SELECT * FROM `" + tableId + "`";
        java.nio.file.Path path = Files.createTempDirectory("bigquery");
        java.nio.file.Path sqlFile = Files.createTempFile(path, "bigquery", ".sql");
        Files.write(sqlFile, sql.getBytes(StandardCharsets.UTF_8));

        Long rowCount = producerTemplate.requestBody(
                "google-bigquery-sql:" + projectId + ":file:" + sqlFile.toAbsolutePath().toString(),
                null, Long.class);
        return Response.ok(rowCount).build();
    }

    @Path("/table")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response dropTable() {
        String sql = "DROP TABLE `" + tableId + "`";
        producerTemplate.requestBody("google-bigquery-sql:" + projectId + ":" + sql, null, Long.class);
        return Response.ok().build();
    }
}
