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
package org.apache.camel.quarkus.component.jasypt.it;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.direct.DirectComponent;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/jasypt")
public class JasyptResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext camelContext;

    @Path("/decrypt/configuration/{configKey}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String decryptConfiguration(
            @PathParam("configKey") String configKey,
            @QueryParam("endpointURI") String endpointURI) {
        return producerTemplate.requestBodyAndHeader(endpointURI, null, "config.key", configKey, String.class);
    }

    @Path("/decrypt/injected/configuration/{endpointUri}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String decryptConfiguration(@PathParam("endpointUri") String endpointUri) {
        return producerTemplate.requestBody(endpointUri, null, String.class);
    }

    @Path("/timer/mock/results")
    @GET
    public void assertMockEndpoint(@QueryParam("expectedMessageCount") int expectedMessageCount) throws Exception {
        MockEndpoint endpoint = camelContext.getEndpoint("mock:timerResult", MockEndpoint.class);
        camelContext.getRouteController().startRoute("secret-timer");
        try {
            String expectedBody = "delay = 1, repeatCount = 2";
            endpoint.expectedMessageCount(expectedMessageCount);
            endpoint.expectedBodiesReceived(expectedBody, expectedBody);
            endpoint.assertIsSatisfied(10000);
        } finally {
            endpoint.reset();
            camelContext.getRouteController().stopRoute("secret-timer");
        }
    }

    @Path("/secure/database")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String connectToSecureDatabase() throws SQLException {
        AgroalDataSource dataSource = Arc.container().instance(AgroalDataSource.class).get();
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery("SELECT USER_NAME FROM INFORMATION_SCHEMA.USERS")) {
                    if (rs.next()) {
                        return rs.getString("USER_NAME").toLowerCase();
                    }
                    return null;
                }
            }
        }
    }

    @Path("/secure/direct/component")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public long directComponentTimeout() {
        DirectComponent component = camelContext.getComponent("direct", DirectComponent.class);
        return component.getTimeout();
    }
}
