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
package org.apache.camel.quarkus.component.jta.it;

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.main.events.AfterStart;
import org.jboss.logging.Logger;

@Path("/jta")
@ApplicationScoped
public class JtaResource {
    private static final Logger LOG = Logger.getLogger(JtaResource.class);

    @Inject
    @DataSource("camel-ds")
    AgroalDataSource dataSource;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    void postConstruct(@Observes AfterStart event) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                LOG.info("Recreating table 'example'");
                try {
                    statement.execute("drop table example");
                } catch (Exception ignored) {
                }
                statement.execute(
                        "create table example (id serial primary key, message varchar(255) not null, origin varchar(255) not null)");
            }
        }
    }

    @Path("/{policy}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(@PathParam("policy") String policy, String message) throws Exception {
        LOG.infof("Sending to jta policy %s: %s", policy, message);
        final String response = producerTemplate.requestBody("direct:" + policy, message, String.class);
        LOG.infof("Got response from jta: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/in_tx/{policy}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response postInTx(@PathParam("policy") String policy, String message) throws Exception {
        return post(policy, message);
    }

    @Path("/route/{route}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response route(@PathParam("route") String route, String message) throws Exception {
        LOG.infof("message is %s", message);
        String response = producerTemplate.requestBody("direct:" + route, message, String.class);
        LOG.infof("Got response from %s: %s", route, response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/mock/{name}/{count}/{timeout}")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String mock(@PathParam("name") String name, @PathParam("count") int count, @PathParam("timeout") int timeout) {
        MockEndpoint mock = context.getEndpoint("mock:" + name, MockEndpoint.class);
        mock.setExpectedMessageCount(count);
        try {
            mock.assertIsSatisfied(timeout);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
        }
        return mock.getExchanges().stream().map(e -> e.getMessage().getBody(String.class)).collect(Collectors.joining(","));
    }
}
