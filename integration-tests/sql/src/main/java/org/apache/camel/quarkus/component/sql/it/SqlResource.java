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

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;
import org.springframework.util.LinkedCaseInsensitiveMap;

@Path("/sql")
@ApplicationScoped
public class SqlResource {

    @Inject
    @DataSource("camel-sql")
    AgroalDataSource dataSource;

    @Inject
    ProducerTemplate producerTemplate;

    @PostConstruct
    public void postConstruct() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS camel");
                statement.execute("CREATE TABLE camel (id int AUTO_INCREMENT, species VARCHAR(255))");
                statement.execute(
                        "CREATE ALIAS ADD_NUMS FOR \"org.apache.camel.quarkus.component.sql.it.storedproc.NumberAddStoredProcedure.addNumbers\"");
            }
        }
    }

    @Path("/get/{species}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCamel(@PathParam("species") String species) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("species", species);

        return producerTemplate.requestBodyAndHeaders("sql:classpath:sql/get-camels.sql",
                null, params,
                String.class);
    }

    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createCamel(String species) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("species", species);

        producerTemplate.requestBodyAndHeaders(
                "sql:INSERT INTO camel (species) VALUES (:#species)", null,
                params);

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
}
