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
package org.apache.camel.quarkus.component.beanio.it;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.json.bind.JsonbBuilder;
import org.apache.camel.quarkus.component.beanio.it.model.Employee;
import org.apache.camel.quarkus.component.beanio.it.model.EmployeeAnnotated;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.apache.camel.quarkus.component.beanio.it.BeanioResource.FORMATTER;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class BeanioTest {
    @ParameterizedTest
    @ValueSource(strings = { "CSV", "DELIMITED", "FIXEDLENGTH", "XML", })
    void marshal(String type) throws Exception {
        List<Employee> employees = new ArrayList<>();
        Employee one = new Employee();
        one.setFirstName("Joe");
        one.setLastName("Smith");
        one.setTitle("Developer");
        one.setSalary(75000);
        one.setHireDate(FORMATTER.parse("2009-10-01"));
        employees.add(one);

        Employee two = new Employee();
        two.setFirstName("Jane");
        two.setLastName("Doe");
        two.setTitle("Architect");
        two.setSalary(80000);
        two.setHireDate(FORMATTER.parse("2008-01-15"));
        employees.add(two);

        Employee three = new Employee();
        three.setFirstName("Jon");
        three.setLastName("Anderson");
        three.setTitle("Manager");
        three.setSalary(85000);
        three.setHireDate(FORMATTER.parse("2007-03-18"));
        employees.add(three);

        String expected = IOUtils.toString(
                BeanioTest.class.getResourceAsStream("/employees-%s.txt".formatted(type.toLowerCase())),
                StandardCharsets.UTF_8);
        if (type.equals("XML")) {
            // Clean up XML to match what is returned from beanio
            expected = expected.replaceAll("(?s)<!--.*?-->", "").replaceAll(">[\\s\r\n]*<", "><");
        }

        String result = RestAssured.given()
                .queryParam("type", type)
                .contentType(ContentType.JSON)
                .body(JsonbBuilder.create().toJson(employees))
                .post("/beanio/marshal")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertEquals(expected.trim(), result.trim());
    }

    @ParameterizedTest
    @ValueSource(strings = { "CSV", "DELIMITED", "FIXEDLENGTH", "XML" })
    void unmarshal(String type) {
        RestAssured.given()
                .queryParam("type", type)
                .contentType(ContentType.TEXT)
                .body(BeanioTest.class.getResourceAsStream("/employees-%s.txt".formatted(type.toLowerCase())))
                .post("/beanio/unmarshal")
                .then()
                .statusCode(200)
                .body(
                        "[0].firstName", is("Joe"),
                        "[0].lastName", is("Smith"),
                        "[0].title", is("Developer"),
                        "[0].salary", is(75000),
                        "[0].hireDate", is("2009-10-01"),
                        "[1].firstName", is("Jane"),
                        "[1].lastName", is("Doe"),
                        "[1].title", is("Architect"),
                        "[1].salary", is(80000),
                        "[1].hireDate", is("2008-01-15"),
                        "[2].firstName", is("Jon"),
                        "[2].lastName", is("Anderson"),
                        "[2].title", is("Manager"),
                        "[2].salary", is(85000),
                        "[2].hireDate", is("2007-03-18"));
    }

    @Test
    void marshalAnnotated() throws Exception {
        List<EmployeeAnnotated> employees = new ArrayList<>();
        EmployeeAnnotated one = new EmployeeAnnotated();
        one.setFirstName("Joe");
        one.setLastName("Smith");
        one.setTitle("Developer");
        one.setSalary(75000);
        one.setHireDate(FORMATTER.parse("2009-10-01"));
        employees.add(one);

        EmployeeAnnotated two = new EmployeeAnnotated();
        two.setFirstName("Jane");
        two.setLastName("Doe");
        two.setTitle("Architect");
        two.setSalary(80000);
        two.setHireDate(FORMATTER.parse("2008-01-15"));
        employees.add(two);

        EmployeeAnnotated three = new EmployeeAnnotated();
        three.setFirstName("Jon");
        three.setLastName("Anderson");
        three.setTitle("Manager");
        three.setSalary(85000);
        three.setHireDate(FORMATTER.parse("2007-03-18"));
        employees.add(three);

        String expected = IOUtils.toString(BeanioTest.class.getResourceAsStream("/employees-csv.txt"),
                StandardCharsets.UTF_8);

        String result = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(JsonbBuilder.create().toJson(employees))
                .post("/beanio/marshal/annotated")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertEquals(expected.trim(), result.trim());
    }

    @Test
    void unmarshalAnnotated() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(BeanioTest.class.getResourceAsStream("/employees-csv.txt"))
                .post("/beanio/unmarshal/annotated")
                .then()
                .statusCode(200)
                .body(
                        "[0].firstName", is("Joe"),
                        "[0].lastName", is("Smith"),
                        "[0].title", is("Developer"),
                        "[0].salary", is(75000),
                        "[0].hireDate", is("2009-10-01"),
                        "[1].firstName", is("Jane"),
                        "[1].lastName", is("Doe"),
                        "[1].title", is("Architect"),
                        "[1].salary", is(80000),
                        "[1].hireDate", is("2008-01-15"),
                        "[2].firstName", is("Jon"),
                        "[2].lastName", is("Anderson"),
                        "[2].title", is("Manager"),
                        "[2].salary", is(85000),
                        "[2].hireDate", is("2007-03-18"));
    }

    @Test
    void marshalSingleObject() {
        Map<String, String> data = Map.of(
                "separator", ":",
                "value", "Content starts from here\nthen continues\nand ends here.",
                "key", "1234");

        String expected = data.get("key") + data.get("separator") + data.get("value");
        String result = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(JsonbBuilder.create().toJson(data))
                .post("/beanio/marshal/single/object")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertEquals(expected, result.trim());
    }

    @Test
    void unmarshalSingleObject() {
        String data = "1234:Content starts from here\nthen continues\nand ends here.";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(data)
                .post("/beanio/unmarshal/single/object")
                .then()
                .statusCode(200)
                .body(
                        "separator", is(":"),
                        "value", is(data.split(":")[1]),
                        "key", is("1234"));

    }

    @Test
    void unmarshalWithErrorHandler() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(BeanioTest.class.getResourceAsStream("/employees-with-error.txt"))
                .post("/beanio/unmarshal/with/error/handler")
                .then()
                .statusCode(200)
                .body(
                        "[0].firstName", is("Joe"),
                        "[0].lastName", is("Smith"),
                        "[0].title", is("Developer"),
                        "[0].salary", is(75000),
                        "[0].hireDate", is("2009-10-01"),
                        "[1].firstName", is("Jane"),
                        "[1].lastName", is("Doe"),
                        "[1].title", is("Architect"),
                        "[1].salary", is(80000),
                        "[1].hireDate", is("2008-01-15"),
                        "[2].message", is("Invalid 'employee' record at line 3"),
                        "[2].record", is("employee"));
    }

    @Test
    void marshalComplexObject() throws Exception {
        String expected = IOUtils.toString(BeanioTest.class.getResourceAsStream("/complex-data.txt"), StandardCharsets.UTF_8);
        String result = RestAssured.given()
                .post("/beanio/marshal/complex/object")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
        assertEquals(expected, result);
    }

    @Test
    void unmarshalComplexObject() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(BeanioTest.class.getResourceAsStream("/complex-data.txt"))
                .post("/beanio/unmarshal/complex/object")
                .then()
                .statusCode(200)
                .body(
                        "[0].identifier", is("A1"),
                        "[0].recordType", is("PRICE"),
                        "[0].date", is("2008-08-03"),
                        "[1].identifier", is("B1"),
                        "[1].recordType", is("SECURITY"),
                        "[1].date", is("2008-08-03"),
                        "[2].value", is("HEADER END"),
                        "[3].price", is("12345.68"),
                        "[3].sedol", is("0001917"),
                        "[3].source", is("camel-beanio"),
                        "[4].price", is("59303290.02"),
                        "[4].sedol", is("0002374"),
                        "[4].source", is("camel-beanio"),
                        "[5].securityName", is("SECURITY ONE"),
                        "[5].sedol", is("0015219"),
                        "[5].source", is("camel-beanio"),
                        "[6].value", is("END OF SECTION 1"),
                        "[7].price", is("0.00"),
                        "[7].sedol", is("0076647"),
                        "[7].source", is("camel-beanio"),
                        "[8].price", is("999999999999.00"),
                        "[8].sedol", is("0135515"),
                        "[8].source", is("camel-beanio"),
                        "[9].securityName", is("SECURITY TWO"),
                        "[9].sedol", is("2000815"),
                        "[9].source", is("camel-beanio"),
                        "[10].securityName", is("SECURITY THR"),
                        "[10].sedol", is("2207122"),
                        "[10].source", is("camel-beanio"),
                        "[11].numberOfRecords", is(7));
    }

    @Test
    void splitter() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(BeanioTest.class.getResourceAsStream("/employees-csv.txt"))
                .post("/beanio/split")
                .then()
                .statusCode(200)
                .body(
                        "[0].firstName", is("Joe"),
                        "[0].lastName", is("Smith"),
                        "[0].title", is("Developer"),
                        "[0].salary", is(75000),
                        "[0].hireDate", is("2009-10-01"),
                        "[1].firstName", is("Jane"),
                        "[1].lastName", is("Doe"),
                        "[1].title", is("Architect"),
                        "[1].salary", is(80000),
                        "[1].hireDate", is("2008-01-15"),
                        "[2].firstName", is("Jon"),
                        "[2].lastName", is("Anderson"),
                        "[2].title", is("Manager"),
                        "[2].salary", is(85000),
                        "[2].hireDate", is("2007-03-18"));
    }
}
