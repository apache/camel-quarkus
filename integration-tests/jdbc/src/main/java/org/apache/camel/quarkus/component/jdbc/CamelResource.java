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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import org.apache.camel.ProducerTemplate;

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
                } catch (Exception ignored) {
                }
                statement.execute("create table camels (id int primary key, species varchar(255))");
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
        return template.requestBody("direct:execute", "select species from camels where id = " + id, String.class);
    }

    @Path("/execute")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String executeStatement(String statement) throws Exception {
        return template.requestBody("direct:execute", statement, String.class);
    }
}
