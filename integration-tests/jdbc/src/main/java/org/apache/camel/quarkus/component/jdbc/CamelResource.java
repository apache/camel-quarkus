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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.jdbc.model.Camel;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/test")
@ApplicationScoped
public class CamelResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelResource.class);
    @Inject
    @DataSource("cameldb")
    AgroalDataSource dataSource;

    @Inject
    ProducerTemplate template;

    @Inject
    CamelContext context;

    @ConfigProperty(name = "quarkus.datasource.cameldb.db-kind")
    String dbKind;

    @PostConstruct
    void postConstruct() throws Exception {
        Connection conn = dataSource.getConnection();
        runScripts(conn, "droptables.sql");
        runScripts(conn, dbKind + ".sql");
        runScripts(conn, "inserts.sql");

        context.getRouteController().startRoute("jdbc-poll");
    }

    @Path("/species/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSpeciesById(@PathParam("id") String id) throws Exception {
        return template.requestBody("jdbc:cameldb", "select species from camels where id = " + id, String.class);
    }

    @SuppressWarnings("unchecked")
    @Path("/species/{id}/list")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSpeciesByIdWithSelectList(@PathParam("id") String id) throws Exception {
        List<LinkedHashMap<String, Object>> result = template
                .requestBody("jdbc:cameldb?outputType=SelectList", "select * from camels where id = " + id, List.class);

        if (result.isEmpty()) {
            throw new IllegalStateException("Expected at least 1 camel result but none were found");
        }

        LinkedHashMap<String, Object> data = result.get(0);
        return data.get(getSpeciesRowName()) + " " + data.get(getIdRowName());
    }

    @SuppressWarnings("unchecked")
    @Path("/species/{id}/type")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSpeciesByIdWithDefinedType(@PathParam("id") String id) throws Exception {
        List<Camel> results = template.requestBody("jdbc:cameldb?outputClass=" + Camel.class.getName(),
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
        return template.requestBody("jdbc:cameldb", statement, String.class);
    }

    @Path("/generated-keys/rows")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List generatedKeysRows() throws Exception {
        return template.requestBodyAndHeader("direct://get-generated-keys",
                "insert into camelsGenerated (species) values ('Camelus testus')",
                "CamelRetrieveGeneratedKeys", "true", ArrayList.class);
    }

    @Path("/headers/insert")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String headersFromInsertOrUpdate() throws Exception {
        return template.requestBodyAndHeader("direct://get-headers",
                "insert into camelsGenerated (species) values ('Camelus testus')",
                "CamelRetrieveGeneratedKeys", "true", String.class);
    }

    @Path("/headers/select")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String headersFromSelect() throws Exception {
        return template.requestBody("direct://get-headers", "select * from camelsGenerated", String.class);
    }

    @Path("/named-parameters/headers-as-parameters")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String headersAsParameters() throws Exception {
        int id = 3;
        return template.requestBodyAndHeader("direct://headers-as-parameters",
                "select * from camels where id < :?idmax order by id",
                "idmax", id, String.class);
    }

    @Path("/named-parameters/headers-as-parameters-map")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String headersAsParametersMap() throws Exception {
        Map<String, Object> headersMap = Map.of("idmax", 3, "specs", "Camelus bactrianus");
        return template.requestBodyAndHeader("direct://headers-as-parameters",
                "select * from camels where id < :?idmax and species = :?specs order by id",
                "CamelJdbcParameters", headersMap, String.class);
    }

    @Path("/interval-polling")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public void intervalPolling(String selectResult) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:interval-polling", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived(selectResult);

        mockEndpoint.assertIsSatisfied();
    }

    @Path("/move-between-datasources")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String moveBetweenDatasources() throws Exception {
        return template.requestBody("direct://move-between-datasources", null, String.class);
    }

    private void runScripts(Connection conn, String fileName) throws SQLException, IOException {
        try (Statement statement = conn.createStatement()) {
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("sql/" + fileName);
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr)) {

                //execute each line from the sql script as separate statement
                reader.lines().filter(s -> s != null && !"".equals(s) && !s.startsWith("--")).forEach(s -> {
                    try {
                        statement.execute(s);
                    } catch (SQLException e) {
                        if (!s.toUpperCase().startsWith("DROP")) {
                            throw new RuntimeException(e);
                        } else {
                            LOGGER.debug(String.format("Command '%s' failed.", s)); //use debug logging
                        }
                    }
                });
            }
        }

    }

    @Path("/get-id-key")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIdKey() {
        switch (dbKind) {
        case "postgresql":
            return "id";
        case "oracle":
            return "ROWID";
        case "mssql":
            return "GENERATED_KEYS";
        case "mariadb":
            return "insert_id";
        case "mysql":
            return "GENERATED_KEY";
        default:
            return "ID";
        }
    }

    private String getIdRowName() {
        if (dbKind.equals("h2") || dbKind.equals("oracle") || dbKind.equals("db2")) {
            return "ID";
        } else {
            return "id";
        }
    }

    private String getSpeciesRowName() {
        if (dbKind.equals("h2") || dbKind.equals("oracle") || dbKind.equals("db2")) {
            return "SPECIES";
        } else {
            return "species";
        }
    }
}
