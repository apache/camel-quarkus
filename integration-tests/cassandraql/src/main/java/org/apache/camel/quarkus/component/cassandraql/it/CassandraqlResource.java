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
package org.apache.camel.quarkus.component.cassandraql.it;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatementBuilder;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.cassandra.CassandraConstants;

@Path("/cassandraql")
@ApplicationScoped
public class CassandraqlResource {
    public static final String EMPTY_LIST = "EMPTY";

    @Inject
    CamelContext camelContext;

    @Inject
    FluentProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/insertEmployee")
    @POST
    public void insertEmployee(
            @QueryParam("name") String name,
            @QueryParam("address") String address,
            @QueryParam("id") int id,
            @QueryParam("endpointUri") String endpointUri) {
        producerTemplate.to(endpointUri)
                .withBody(new Employee(id, name, address).getValueForInsert())
                .request();
    }

    @Path("/getEmployee")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("unchecked")
    public String getEmployee(@QueryParam("id") int id) {
        final List<Row> rows = producerTemplate
                .to("direct:read")
                .withBody(id)
                .request(List.class);
        return convertBodyToString(rows);
    }

    @Path("/cqlHeaderQuery")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("unchecked")
    public String cqlHeaderQuery(
            @QueryParam("id") int id,
            @QueryParam("cql") String cql,
            @QueryParam("queryAsSimpleStatement") boolean queryAsSimpleStatement) {

        Object cqlQuery;
        if (queryAsSimpleStatement) {
            cqlQuery = new SimpleStatementBuilder(cql).build();
        } else {
            cqlQuery = cql;
        }

        final List<Row> rows = producerTemplate
                .to("direct:cqlHeaderQuery")
                .withHeader(CassandraConstants.CQL_QUERY, cqlQuery)
                .withBody(id)
                .request(List.class);
        return convertBodyToString(rows);
    }

    @Path("/getEmployeeWithStrategy")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("unchecked")
    public String getEmployeeWithStrategy(@QueryParam("id") int id) {
        final List<Employee> employees = producerTemplate
                .to("direct:readWithCustomStrategy")
                .withBody(id)
                .request(List.class);

        if (employees.isEmpty()) {
            return "";
        }
        return employees.get(0).getName();
    }

    @Path("/getAllEmployees")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getAllEmployees() throws Exception {
        try {
            camelContext.getRouteController().startRoute("employee-consumer");
            Exchange exchange = consumerTemplate.receive("seda:employees", 5000);
            return convertBodyToString(exchange.getMessage().getBody());
        } finally {
            camelContext.getRouteController().stopRoute("employee-consumer");
        }
    }

    @Path("/updateEmployee")
    @PATCH
    @Produces(MediaType.TEXT_PLAIN)
    public boolean updateEmployee(
            @QueryParam("name") String name,
            @QueryParam("address") String address,
            @QueryParam("id") int id) {
        final Object result = producerTemplate.to("direct:update")
                .withBody(new Employee(id, name, address).getValueForUpdate())
                .request();
        return result != null;
    }

    @Path("/deleteEmployeeById")
    @DELETE
    public void deleteEmployeeById(@QueryParam("id") int id) {
        producerTemplate.to("direct:delete")
                .withBody(id)
                .request();
    }

    @Path("/aggregate")
    @POST
    public void aggregate(
            @QueryParam("name") String name,
            @QueryParam("address") String address,
            @QueryParam("id") int id) {
        producerTemplate.to("direct:aggregate")
                .withBody(new Employee(id, name, address))
                .request();
    }

    @Path("/checkLoadBalancingPolicy")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean checkLoadBalancingPolicy() throws Exception {
        return CustomLoadBalancingPolicy.awaitInitialization();
    }

    @SuppressWarnings("unchecked")
    private String convertBodyToString(Object body) {
        if (body instanceof List) {
            if (((List<?>) body).isEmpty()) {
                return EMPTY_LIST;
            } else {
                return ((List<Row>) body).stream()
                        .map(Row::getFormattedContents)
                        .collect(Collectors.joining(";"));
            }
        } else if (body instanceof String) {
            return (String) body;
        }
        return "";
    }
}
