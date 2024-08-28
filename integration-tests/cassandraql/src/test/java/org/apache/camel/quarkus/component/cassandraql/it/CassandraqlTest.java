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

import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.apache.camel.quarkus.component.cassandraql.it.CassandraqlResource.EMPTY_LIST;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@QuarkusTestResource(CassandraqlTestResource.class)
class CassandraqlTest {

    private Employee sheldon = new Employee(1, "Sheldon", "Alpha Centauri");
    private Employee leonard = new Employee(2, "Leonard", "Earth");
    private Employee irma = new Employee(3, "Irma", "Jupiter");

    @Test
    public void testCassandraCrudOperations() throws Exception {
        try {
            // Create / read
            Stream.of(sheldon, leonard, irma).forEach(employee -> {
                insertEmployee(employee, "direct:create");
                assertEmployee(employee.getId(), containsString(employee.getName()));
            });

            // Read all
            assertAllEmployees(allOf(
                    containsString(sheldon.getName()),
                    containsString(leonard.getName()),
                    containsString(irma.getName())));

            // Update
            Employee updatedSheldon = (Employee) sheldon.clone();
            updatedSheldon.setAddress("Earth 2.0");

            assertEmployee(sheldon.getId(), both(containsString(sheldon.getAddress()))
                    .and(not(containsString(updatedSheldon.getAddress()))));
            updateEmployee(updatedSheldon);
            assertEmployee(sheldon.getId(), both(containsString(updatedSheldon.getAddress()))
                    .and(not(containsString(sheldon.getAddress()))));
        } finally {
            // Delete
            Stream.of(sheldon, leonard, irma).forEach(employee -> {
                deleteEmployee(employee.getId());
                assertEmployee(employee.getId(), equalTo(EMPTY_LIST));
            });
        }
    }

    @Test
    public void customSession() {
        try {
            Stream.of(sheldon, leonard, irma).forEach(employee -> {
                insertEmployee(employee, "direct:createCustomSession");
            });

            assertAllEmployees(allOf(
                    containsString(sheldon.getName()),
                    containsString(leonard.getName()),
                    containsString(irma.getName())));
        } finally {
            Stream.of(sheldon, leonard, irma).forEach(employee -> {
                deleteEmployee(employee.getId());
            });
        }
    }

    @Test
    public void quarkusCassandraSession() {
        try {
            Stream.of(sheldon, leonard, irma).forEach(employee -> {
                insertEmployee(employee, "direct:createQuarkusSession");
            });

            assertAllEmployees(allOf(
                    containsString(sheldon.getName()),
                    containsString(leonard.getName()),
                    containsString(irma.getName())));
        } finally {
            Stream.of(sheldon, leonard, irma).forEach(employee -> {
                deleteEmployee(employee.getId());
            });
        }
    }

    @Test
    public void idempotent() {
        try {
            for (int i = 0; i < 5; i++) {
                insertEmployee(sheldon, "direct:createIdempotent");
            }

            assertAllEmployees(equalTo("[id:1, address:'Alpha Centauri', name:'Sheldon']"));
        } finally {
            deleteEmployee(sheldon.getId());
        }
    }

    @Test
    public void aggregate() {
        Stream.of("foo", "bar", "cheese").forEach(name -> {
            Employee employee = new Employee(1, name, name + " address");
            RestAssured.given()
                    .queryParam("id", employee.getId())
                    .queryParam("name", employee.getName())
                    .queryParam("address", employee.getAddress())
                    .post("/cassandraql/aggregate")
                    .then()
                    .statusCode(204);
        });

        assertAllEmployees(equalTo("foo,bar,cheese"));
    }

    @Test
    public void resultSetConversionStrategy() {
        try {
            insertEmployee(sheldon, "direct:create");
            assertEmployee(sheldon.getId(), containsString(sheldon.getName()));

            RestAssured.given()
                    .queryParam("id", sheldon.getId())
                    .get("/cassandraql/getEmployeeWithStrategy")
                    .then()
                    .statusCode(200)
                    .body(equalTo(sheldon.getName() + " modified"));
        } finally {
            deleteEmployee(sheldon.getId());
        }
    }

    @Test
    public void loadBalancingPolicy() {
        try {
            insertEmployee(sheldon, "direct:createCustomLoadBalancingPolicy");
            RestAssured.given()
                    .get("/cassandraql/checkLoadBalancingPolicy")
                    .then()
                    .statusCode(200)
                    .body(equalTo("true"));
        } finally {
            deleteEmployee(sheldon.getId());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void cqlStatementViaHeader(boolean queryAsSimpleStatement) {
        try {
            insertEmployee(sheldon, "direct:create");

            RestAssured.given()
                    .queryParam("id", sheldon.getId())
                    .queryParam("cql", "SELECT * FROM employee WHERE id = ?")
                    .queryParam("queryAsSimpleStatement", queryAsSimpleStatement)
                    .get("/cassandraql/cqlHeaderQuery")
                    .then()
                    .statusCode(200)
                    .body(equalTo(("[id:1, address:'Alpha Centauri', name:'Sheldon']")));
        } finally {
            deleteEmployee(sheldon.getId());
        }
    }

    private void assertEmployee(int id, Matcher<?> matcher) {
        RestAssured.given()
                .queryParam("id", id)
                .get("/cassandraql/getEmployee")
                .then()
                .statusCode(200)
                .body(matcher);
    }

    private void assertAllEmployees(Matcher<?> matcher) {
        RestAssured
                .get("/cassandraql/getAllEmployees")
                .then()
                .statusCode(200)
                .body(matcher);
    }

    private void insertEmployee(Employee employee, String endpointUri) {
        RestAssured.given()
                .queryParam("id", employee.getId())
                .queryParam("name", employee.getName())
                .queryParam("address", employee.getAddress())
                .queryParam("endpointUri", endpointUri)
                .post("/cassandraql/insertEmployee")
                .then()
                .statusCode(204);
    }

    private void updateEmployee(Employee employee) {
        RestAssured.given()
                .queryParam("id", employee.getId())
                .queryParam("name", employee.getName())
                .queryParam("address", employee.getAddress())
                .patch("/cassandraql/updateEmployee")
                .then()
                .statusCode(200)
                .body(equalTo(String.valueOf(true)));
    }

    private void deleteEmployee(int id) {
        RestAssured.given()
                .queryParam("id", id)
                .delete("/cassandraql/deleteEmployeeById")
                .then()
                .statusCode(204);
    }

}
