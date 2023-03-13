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
package org.apache.camel.quarkus.component.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.jdbc.model.Camel;

@Path("/test")
@ApplicationScoped
public class CamelResource {
    @Inject
    @DataSource("camel-ds")
    AgroalDataSource dataSource;

    @Inject
    ProducerTemplate template;

    @PostConstruct
    void postConstruct() throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                try {
                    statement.execute("drop table camels");
                    statement.execute("drop table camelsGenerated");
                } catch (Exception ignored) {
                }
                statement.execute("create table camels (id int primary key, species varchar(255))");
                statement.execute("create table camelsGenerated (id int primary key auto_increment, species varchar(255))");
                statement.execute("insert into camels (id, species) values (1, 'Camelus dromedarius')");
                statement.execute("insert into camels (id, species) values (2, 'Camelus bactrianus')");
                statement.execute("insert into camels (id, species) values (3, 'Camelus ferus')");
            }
        }
    }

    @Path("/species/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSpeciesById(@PathParam("id") String id) throws Exception {
        return template.requestBody("jdbc:camel-ds", "select species from camels where id = " + id, String.class);
    }

    @SuppressWarnings("unchecked")
    @Path("/species/{id}/list")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSpeciesByIdWithSelectList(@PathParam("id") String id) throws Exception {
        List<LinkedHashMap<String, Object>> result = template
                .requestBody("jdbc:camel-ds?outputType=SelectList", "select * from camels where id = " + id, List.class);

        if (result.isEmpty()) {
            throw new IllegalStateException("Expected at least 1 camel result but none were found");
        }

        LinkedHashMap<String, Object> data = result.get(0);
        return data.get("SPECIES") + " " + data.get("ID");
    }

    @SuppressWarnings("unchecked")
    @Path("/species/{id}/type")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSpeciesByIdWithDefinedType(@PathParam("id") String id) throws Exception {
        List<Camel> results = template.requestBody("jdbc:camel-ds?outputClass=" + Camel.class.getName(),
                "select * from camels where id = " + id, List.class);

        if (results.isEmpty()) {
            throw new IllegalStateException("Expected at least 1 camel result but none were found");
        }

        Camel camel = results.get(0);
        return camel.getSpecies() + " " + camel.getId();
    }

    @Path("/execute")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String executeStatement(String statement) throws Exception {
        return template.requestBody("jdbc:camel-ds", statement, String.class);
    }

    @Path("/generated-keys/rows")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List generatedKeysRows() throws Exception {
        return template.requestBodyAndHeader("direct://get-generated-keys",
                "insert into camelsGenerated (species) values ('Camelus testus'), ('Camelus legendarius')",
                "CamelRetrieveGeneratedKeys", "true", ArrayList.class);
    }

    @Path("/headers/insert")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String headersFromInsertOrUpdate() throws Exception {
        return template.requestBodyAndHeader("direct://get-headers",
                "insert into camelsGenerated (species) values ('Camelus status'), ('Camelus linus')",
                "CamelRetrieveGeneratedKeys", "true", String.class);
    }

    @Path("/headers/select")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String headersFromSelect() throws Exception {
        return template.requestBody("direct://get-headers", "select * from camelsGenerated", String.class);
    }
}
