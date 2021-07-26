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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Order;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@QuarkusTestResource(CassandraqlTestResource.class)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CassandraqlTest {

    private Employee sheldon = new Employee(1, "Sheldon", "Alpha Centauri");
    private Employee leonard = new Employee(2, "Leonard", "Earth");
    private Employee irma = new Employee(3, "Irma", "Jupiter");

    //@Test
    @Order(1)
    public void testInsert() {
        insertEmployee(sheldon);
        assertEmployee(sheldon.getId(), containsString(sheldon.getName()));

        insertEmployee(leonard);
        assertEmployee(leonard.getId(), containsString(leonard.getName()));

        assertEmployee(irma.getId(), equalTo(CassandraqlResource.EMPTY_LIST));

        insertEmployee(irma);
        assertEmployee(irma.getId(), containsString(irma.getName()));
    }

    //@Test
    @Order(2)
    public void testUpdate() throws CloneNotSupportedException {
        Employee updatedSheldon = (Employee) sheldon.clone();
        updatedSheldon.setAddress("Earth 2.0");

        assertEmployee(sheldon.getId(), both(containsString(sheldon.getAddress()))
                .and(not(containsString(updatedSheldon.getAddress()))));
        updateEmployee(updatedSheldon);
        assertEmployee(sheldon.getId(), both(containsString(updatedSheldon.getAddress()))
                .and(not(containsString(sheldon.getAddress()))));
    }

    //@Test
    @Order(3)
    public void testDelete() {
        assertAllEmployees(allOf(
                containsString(sheldon.getName()),
                containsString(leonard.getName()),
                containsString(irma.getName())));
        deleteEmployee(sheldon.getId());
        deleteEmployee(leonard.getId());
        assertAllEmployees(allOf(
                not(containsString(sheldon.getName())),
                not(containsString(leonard.getName())),
                containsString(irma.getName())));
        deleteEmployee(irma.getId());
        assertAllEmployees(equalTo(CassandraqlResource.EMPTY_LIST));
    }

    private void assertEmployee(int id, Matcher matcher) {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(id)
                .post("/cassandraql/getEmployee")
                .then()
                .statusCode(200)
                .body(matcher);
    }

    private void assertAllEmployees(Matcher matcher) {
        RestAssured
                .get("/cassandraql/getAllEmployees")
                .then()
                .statusCode(200)
                .body(matcher);
    }

    private void insertEmployee(Employee employee) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(employee)
                .post("/cassandraql/insertEmployee")
                .then()
                .statusCode(204);
    }

    private void updateEmployee(Employee employee) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(employee)
                .post("/cassandraql/updateEmployee")
                .then()
                .statusCode(200)
                .body(equalTo(String.valueOf(true)));
    }

    private void deleteEmployee(int id) {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(id)
                .post("/cassandraql/deleteEmployeeById")
                .then()
                .statusCode(204);
    }

}
