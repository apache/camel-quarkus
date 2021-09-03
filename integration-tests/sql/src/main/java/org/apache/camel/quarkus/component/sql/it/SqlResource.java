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
package org.apache.camel.quarkus.component.sql.it;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.agroal.api.AgroalDataSource;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.sql.SqlConstants;
import org.apache.camel.quarkus.component.sql.it.model.Camel;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.springframework.util.LinkedCaseInsensitiveMap;

@Path("/sql")
@ApplicationScoped
public class SqlResource {

    @ConfigProperty(name = "quarkus.datasource.db-kind")
    String dbKind;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    @Named("results")
    Map<String, List> results;

    @Inject
    CamelContext camelContext;

    @Path("/get/{species}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCamel(@PathParam("species") String species) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("species", species);

        return producerTemplate.requestBodyAndHeaders("sql:classpath:sql/common/get-camels.sql",
                null, params,
                String.class);
    }

    @SuppressWarnings("unchecked")
    @Path("/get/{species}/list")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCamelSelectList(@PathParam("species") String species) throws Exception {
        List<LinkedCaseInsensitiveMap<Object>> result = producerTemplate.requestBody(
                "sql:SELECT * FROM camel WHERE species = '" + species + "'?outputType=SelectList",
                null,
                List.class);

        if (result.isEmpty()) {
            throw new IllegalStateException("Expected at least 1 camel result but none were found");
        }

        LinkedCaseInsensitiveMap<Object> data = result.get(0);
        return data.get("SPECIES") + " " + data.get("ID");
    }

    @SuppressWarnings("unchecked")
    @Path("/get/{species}/list/type")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCamelSelectListWithType(@PathParam("species") String species) throws Exception {
        List<Camel> camels = producerTemplate.requestBody("sql:SELECT * FROM camel WHERE "
                + "species = '" + species + "'?outputType=SelectList"
                + "&outputClass=org.apache.camel.quarkus.component.sql.it.model.Camel",
                null,
                List.class);

        if (camels.isEmpty()) {
            throw new IllegalStateException("Expected at least 1 camel result but none were found");
        }

        Camel result = camels.get(0);
        return result.getSpecies() + " " + result.getId();
    }

    @Path("/insert/")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("unchecked")
    public Response insert(@QueryParam("table") String table, Map<String, Object> values) throws Exception {
        LinkedHashMap linkedHashMap = new LinkedHashMap(values);

        String sql = String.format("sql:INSERT INTO %s (%s) VALUES (%s)", table,
                linkedHashMap.keySet().stream().collect(Collectors.joining(", ")),
                linkedHashMap.keySet().stream().map(s -> ":#" + s).collect(Collectors.joining(", ")));

        producerTemplate.requestBodyAndHeaders(sql, null, values);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/update/")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response update(@QueryParam("table") String table, Map<String, Object> values) throws Exception {

        String sql = String.format("sql:update %s set %s where id=:#id", table,
                values.keySet().stream()
                        .filter(k -> !"ID".equals(k))
                        .map(k -> k + " = :#" + k)
                        .collect(Collectors.joining(", ")));

        producerTemplate.requestBodyAndHeaders(sql, null, values);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @SuppressWarnings("unchecked")
    @Path("/storedproc")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String callStoredProcedure(@QueryParam("numA") int numA, @QueryParam("numB") int numB) {
        Map<String, Object> args = new HashMap<>();
        args.put("num1", numA);
        args.put("num2", numB);

        Map<String, List<LinkedCaseInsensitiveMap>> results = producerTemplate
                .requestBodyAndHeaders("sql-stored:ADD_NUMS(INTEGER ${headers.num1},INTEGER ${headers.num2})", null, args,
                        Map.class);

        return results.get("#result-set-1").get(0).get("PUBLIC.ADD_NUMS(?1, ?2)").toString();
    }

    @Path("/get/results/{resultId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List consumerResults(@PathParam("resultId") String resultId) throws Exception {
        List<Map> list = new LinkedList(this.results.get(resultId));
        results.get(resultId).clear();
        return list;
    }

    @Path("/toDirect/{directId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object toDirect(@PathParam("directId") String directId, @QueryParam("body") String body, Map<String, Object> headers)
            throws Exception {
        String sql = (String) headers.get(SqlConstants.SQL_QUERY);
        if (sql != null) {
            headers.put(SqlConstants.SQL_QUERY,
                    sql.replaceAll("BOOLEAN_FALSE", SqlHelper.convertBooleanToSqlDialect(dbKind, false)));
        }

        try {
            return producerTemplate.requestBodyAndHeaders("direct:" + directId, body, headers, Object.class);
        } catch (CamelExecutionException e) {
            return e.getCause().getClass().getName() + ":" + e.getCause().getMessage();
        }
    }

}
