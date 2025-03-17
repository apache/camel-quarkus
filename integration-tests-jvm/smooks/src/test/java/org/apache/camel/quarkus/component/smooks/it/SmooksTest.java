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
package org.apache.camel.quarkus.component.smooks.it;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.smooks.it.model.Customer;
import org.apache.camel.quarkus.component.smooks.it.model.Gender;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class SmooksTest {

    @Test
    void beanRouting() throws IOException {
        try (InputStream stream = SmooksTest.class.getResourceAsStream("/customer.xml")) {
            if (stream == null) {
                throw new IllegalStateException("Cannot load customer.xml");
            }

            String customerCountryUSXml = new String(stream.readAllBytes());
            RestAssured.given()
                    .contentType("application/xml")
                    .body(customerCountryUSXml)
                    .when()
                    .post("/smooks/bean/routing")
                    .then()
                    .statusCode(204);

            String customerCountryUKXml = customerCountryUSXml.replace("US", "UK");
            RestAssured.given()
                    .contentType("application/xml")
                    .body(customerCountryUKXml)
                    .when()
                    .post("/smooks/bean/routing")
                    .then()
                    .statusCode(204);

            Stream.of("US", "UK").forEach(country -> {
                RestAssured.get("/smooks/bean/routing/endpoint/" + "seda:country" + country)
                        .then()
                        .statusCode(200)
                        .body(
                                "firstName", equalTo("John"),
                                "lastName", equalTo("Doe"),
                                "gender", equalTo(Gender.Male.name()),
                                "age", equalTo(37),
                                "country", equalTo(country));
            });
        }
    }

    @Test
    void dataFormatMarshal() {
        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setGender(Gender.Male);
        customer.setAge(37);
        customer.setCountry("US");

        String rootTagName = Customer.class.getSimpleName();
        RestAssured.given()
                .contentType("application/json")
                .body(customer)
                .post("/smooks/dataformat/marshal")
                .then()
                .statusCode(200)
                .body(
                        rootTagName + ".firstName", equalTo("John"),
                        rootTagName + ".lastName", equalTo("Doe"),
                        rootTagName + ".gender", equalTo(Gender.Male.name()),
                        rootTagName + ".age", equalTo("37"),
                        rootTagName + ".country", equalTo("US"));
    }

    @Test
    void dataFormatUnmarshal() throws IOException {
        try (InputStream stream = SmooksTest.class.getResourceAsStream("/customer.xml")) {
            if (stream == null) {
                throw new IllegalStateException("Cannot load expected-customer.xml");
            }

            RestAssured.given()
                    .contentType("application/xml")
                    .body(new String(stream.readAllBytes()))
                    .post("/smooks/dataformat/unmarshal")
                    .then()
                    .statusCode(200)
                    .body(
                            "firstName", equalTo("John"),
                            "lastName", equalTo("Doe"),
                            "gender", equalTo(Gender.Male.name()),
                            "age", equalTo(37),
                            "country", equalTo("US"));
        }
    }
}
