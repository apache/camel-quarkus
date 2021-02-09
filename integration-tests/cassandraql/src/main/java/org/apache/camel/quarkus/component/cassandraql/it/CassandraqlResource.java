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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.datastax.oss.driver.internal.core.cql.DefaultRow;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;

@Path("/cassandraql")
@ApplicationScoped
public class CassandraqlResource {
    public static final String DB_URL_PARAMETER = CassandraqlResource.class.getSimpleName() + "_db_url";
    public static final String KEYSPACE = "test";
    public static final String EMPTY_LIST = "EMPTY";

    @Inject
    FluentProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/insertEmployee")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void insertEmployee(Employee object) {
        producerTemplate.toF(createUrl("INSERT INTO employee(id, name, address) VALUES (?, ?, ?)"))
                .withBody(object.getValue())
                .request();
    }

    @Path("/getEmployee")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String getEmployee(String id) throws Exception {
        final Exchange exchange = consumerTemplate
                .receive(createUrl(String.format("SELECT * FROM employee WHERE id = %s", id)));
        return convertBodyToString(exchange.getIn().getBody());
    }

    @Path("/getAllEmployees")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getAllEmployees() throws Exception {
        final Exchange exchange = consumerTemplate.receive(createUrl("SELECT id, name, address FROM employee"));
        return convertBodyToString(exchange.getIn().getBody());
    }

    @Path("/updateEmployee")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean updateEmployee(Employee employee) throws Exception {
        final Exchange exchange = consumerTemplate
                .receive(createUrl(String.format("UPDATE employee SET name = '%s', address = '%s' WHERE id = %s",
                        employee.getName(), employee.getAddress(), employee.getId())));
        return exchange != null;
    }

    @Path("/deleteEmployeeById")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void deleteEmplyeeById(String id) throws Exception {
        consumerTemplate.receive(createUrl(String.format("DELETE FROM employee WHERE id = %s", id)));
    }

    private String createUrl(String cql) {
        String url = System.getProperty(DB_URL_PARAMETER);
        return String.format("cql://%s/%s?cql=%s", url, KEYSPACE, cql);
    }

    private String convertBodyToString(Object body) {
        if (body instanceof List) {
            if (((List) body).isEmpty()) {
                return EMPTY_LIST;
            } else {
                return ((List<DefaultRow>) body).stream()
                        .map(r -> r.getFormattedContents())
                        .collect(Collectors.joining(";"));
            }
        }
        return "";
    }
}
