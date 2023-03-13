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
package org.apache.camel.quarkus.component.jdbc;

import java.util.List;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
public class CamelJdbcTest {

    @Test
    void testGetSpeciesById() {
        RestAssured.when().get("/test/species/1").then().body(is("[{SPECIES=Camelus dromedarius}]"));
        RestAssured.when().get("/test/species/2").then().body(is("[{SPECIES=Camelus bactrianus}]"));
        RestAssured.when().get("/test/species/3").then().body(is("[{SPECIES=Camelus ferus}]"));
    }

    @Test
    void testGetSpeciesByIdWithResultList() {
        RestAssured.when().get("/test/species/1/list").then().body(is("Camelus dromedarius 1"));
    }

    @Test
    void testGetSpeciesByIdWithDefinedType() {
        RestAssured.when().get("/test/species/1/type").then().body(is("Camelus dromedarius 1"));
    }

    @Test
    void testExecuteStatement() {
        RestAssured.given()
                .contentType(ContentType.TEXT).body("select id from camels order by id desc")
                .post("/test/execute")
                .then().body(is("[{ID=3}, {ID=2}, {ID=1}]"));
    }

    @Test
    void testCamelRetrieveGeneratedKeysHeader() {
        List generatedIDs = RestAssured.given()
                .get("test/generated-keys/rows")
                .then().extract().body()
                .jsonPath().getList("ID");

        String selectResult = RestAssured.given()
                .contentType(ContentType.TEXT).body("select id from camelsGenerated")
                .post("/test/execute")
                .then().extract().body().asString();

        generatedIDs.forEach(generatedID -> assertTrue(selectResult.contains(generatedID.toString())));
    }

    @Test
    void testHeadersFromInsertOrUpdateQuery() {
        RestAssured.given()
                .get("test/headers/insert")
                .then()
                .body(containsString("CamelGeneratedKeysRowCount=2"))
                .and()
                .body(containsString("CamelJdbcUpdateCount=2"))
                .and()
                .body(containsString("CamelRetrieveGeneratedKeys=true"))
                .and()
                .body(not(containsString("CamelJdbcRowCount")))
                .and()
                .body(not(containsString("CamelJdbcColumnNames")))
                .and()
                .body(not(containsString("CamelJdbcParameters")))
                .and()
                .body(not(containsString("CamelGeneratedColumns")));
    }

    @Test
    void testHeadersFromSelectQuery() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("insert into camelsGenerated (species) values ('Camelus status'), ('Camelus linus')")
                .post("/test/execute");

        RestAssured.given()
                .get("test/headers/select")
                .then()
                .body(not(containsString("CamelGeneratedKeysRowCount")))
                .and()
                .body(not(containsString("CamelJdbcUpdateCount")))
                .and()
                .body(not(containsString("CamelRetrieveGeneratedKeys")))
                .and()
                .body(not(containsString("CamelJdbcParameters")))
                .and()
                .body(not(containsString("CamelGeneratedColumns")))
                .and()
                .body(containsString("CamelJdbcRowCount=2"))
                .and()
                .body(containsString("CamelJdbcColumnNames=[ID, SPECIES]"));
    }
}
