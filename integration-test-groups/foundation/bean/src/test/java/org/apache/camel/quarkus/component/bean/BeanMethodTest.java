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
package org.apache.camel.quarkus.component.bean;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.bean.model.Employee;
import org.awaitility.Awaitility;

@QuarkusTest
public class BeanMethodTest {

    private static final List<Employee> EMPLOYEES = Arrays.asList(
            new Employee("Michael", "Singer", "junior"),
            new Employee("Joe", "Doe", "junior"),
            new Employee("Susanne", "First", "senior"),
            new Employee("Max", "Mustermann", "senior"));

    static void assertFilterAndExpression(String route, String... expectedNames) {
        for (Employee employee : EMPLOYEES) {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(employee)
                    .post("/bean-method/employee/" + route)
                    .then()
                    .statusCode(201);
        }

        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(30, TimeUnit.SECONDS)
                .<String[]> until(
                        () -> RestAssured.given()
                                .get("/bean-method/collectedNames/" + route)
                                .then()
                                .statusCode(200)
                                .extract().body().as(String[].class),
                        names -> Arrays.equals(names, expectedNames));
    }

    //@Test
    public void beanFromRegistryByName() {
        assertFilterAndExpression("beanFromRegistryByName", "Susanne", "Max");
    }

    //@Test
    public void beanByClassName() {
        assertFilterAndExpression("beanByClassName", "Singer", "Doe");
    }

    //@Test
    public void beanInstance() {
        assertFilterAndExpression("beanInstance", "Singer", "Doe");
    }

}
