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
package org.apache.camel.quarkus.component.management.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class ManagementTest {

    @ParameterizedTest
    @ValueSource(strings = { "components", "consumers", "context", "dataformats", "endpoints", "processors", "routes",
            "services" })
    public void testManagementObjects(String type) {
        String contextName = RestAssured.get("/management/context/name")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Look up an object instance by type
        // The CamelId attribute is common to all managed Camel objects,
        // and should match the name of the CamelContext.
        String name = "org.apache.camel:type=" + type + ",*";
        RestAssured.given()
                .queryParam("name", name)
                .queryParam("attribute", "CamelId")
                .get("/management/attribute")
                .then()
                .statusCode(200)
                .body(is(contextName));
    }

    @Test
    public void testDumpRoutesAsXml() {
        RestAssured.given()
                .queryParam("name", "org.apache.camel:type=context,*")
                .queryParam("operation", "dumpRoutesAsXml")
                .post("/management/invoke")
                .then()
                .statusCode(200)
                .body(containsString("<from id=\"from1\" uri=\"direct:start\"/>"));
    }

    @Test
    public void testManagedBean() {
        String name = "org.apache.camel:type=processors,name=\"counter\",*";

        // Counter should be initialized to 0
        RestAssured.given()
                .queryParam("name", name)
                .queryParam("attribute", "Count")
                .get("/management/attribute")
                .then()
                .statusCode(200)
                .body(is("0"));

        // Calling the increment() method should set counter to 1
        RestAssured.given()
                .queryParam("name", name)
                .queryParam("operation", "increment")
                .post("/management/invoke")
                .then()
                .statusCode(200);

        RestAssured.given()
                .queryParam("name", name)
                .queryParam("attribute", "Count")
                .get("/management/attribute")
                .then()
                .statusCode(200)
                .body(is("1"));

        // Call the "direct:count" endpoint to increment the counter
        RestAssured.given()
                .queryParam("endpointUri", "direct:count")
                .post("/management/invoke/route")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("name", name)
                .queryParam("attribute", "Count")
                .get("/management/attribute")
                .then()
                .statusCode(200)
                .body(is("2"));
    }
}
