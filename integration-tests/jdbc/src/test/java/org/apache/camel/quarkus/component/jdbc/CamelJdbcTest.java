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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@DisabledIfSystemProperty(named = "cq.jdbcKind", matches = "derby")
//https://github.com/quarkusio/quarkus/issues/23083
public class CamelJdbcTest {
    @Test
    void testGetSpeciesById() {
        RestAssured.when().get("/test/species/1").then().body(equalToIgnoringCase("[{SPECIES=Camelus dromedarius}]"));
        RestAssured.when().get("/test/species/2").then().body(equalToIgnoringCase("[{SPECIES=Camelus bactrianus}]"));
        RestAssured.when().get("/test/species/3").then().body(equalToIgnoringCase("[{SPECIES=Camelus ferus}]"));
    }

    @Test
    void testGetSpeciesByIdWithResultList() {
        RestAssured.when().get("/test/species/1/list").then().body(is("Camelus dromedarius 1"));
    }

    @Test
    void testGetSpeciesByIdWithDefinedType() {
        RestAssured.when().get("/test/species/1/type").then().body(equalToIgnoringCase("Camelus dromedarius 1"));
    }

    @Test
    void testExecuteStatement() {
        RestAssured.given()
                .contentType(ContentType.TEXT).body("select id from camels order by id desc")
                .post("/test/execute")
                .then().body(equalToIgnoringCase("[{ID=3}, {ID=2}, {ID=1}]"));
    }

    @Test
    void testCamelRetrieveGeneratedKeysHeader() {
        String idKey = RestAssured.when()
                .get("/test/get-id-key")
                .then()
                .extract()
                .body().asString();

        List generatedIDs = RestAssured.given()
                .get("test/generated-keys/rows")
                .then().extract().body()
                .jsonPath().getList(idKey);

        assertFalse(generatedIDs.isEmpty());
        assertNotNull(generatedIDs.get(0));

    }

    @Test
    void testHeadersFromInsertOrUpdateQuery() {
        RestAssured.given()
                .get("test/headers/insert")
                .then()
                .body(containsStringIgnoringCase("CamelGeneratedKeysRowCount=1"))
                .and()
                .body(containsStringIgnoringCase("CamelJdbcUpdateCount=1"))
                .and()
                .body(containsStringIgnoringCase("CamelRetrieveGeneratedKeys=true"))
                .and()
                .body(not(containsStringIgnoringCase("CamelJdbcRowCount")))
                .and()
                .body(not(containsStringIgnoringCase("CamelJdbcColumnNames")))
                .and()
                .body(not(containsStringIgnoringCase("CamelJdbcParameters")))
                .and()
                .body(not(containsStringIgnoringCase("CamelGeneratedColumns")));
    }

    @Test
    void testHeadersFromSelectQuery() {
        RestAssured.given()
                .get("test/headers/select")
                .then()
                .body(not(containsStringIgnoringCase("CamelGeneratedKeysRowCount")))
                .and()
                .body(not(containsStringIgnoringCase("CamelJdbcUpdateCount")))
                .and()
                .body(not(containsStringIgnoringCase("CamelRetrieveGeneratedKeys")))
                .and()
                .body(not(containsStringIgnoringCase("CamelJdbcParameters")))
                .and()
                .body(not(containsStringIgnoringCase("CamelGeneratedColumns")))
                .and()
                .body(containsStringIgnoringCase("CamelJdbcRowCount"))
                .and()
                .body(containsStringIgnoringCase("CamelJdbcColumnNames=[ID, SPECIES]"));
    }

    @Test
    void testNamedParameters() {
        RestAssured.given()
                .get("test/named-parameters/headers-as-parameters")
                .then()
                .body(containsStringIgnoringCase("{ID=1, SPECIES=Camelus dromedarius}"))
                .and()
                .body(containsStringIgnoringCase("{ID=2, SPECIES=Camelus bactrianus}"));
    }

    @Test
    void testCamelJdbcParametersHeader() {
        RestAssured.given()
                .get("test/named-parameters/headers-as-parameters-map")
                .then()
                .body(containsStringIgnoringCase("{ID=2, SPECIES=Camelus bactrianus}"));
    }

    @Test
    void testTimeIntervalDatabasePolling() {
        String selectResult = RestAssured.given()
                .contentType(ContentType.TEXT).body("select * from camelsGenerated order by id desc")
                .post("/test/execute")
                .then().extract().body().asString();

        RestAssured.given()
                .body(selectResult)
                .get("/test/interval-polling")
                .then()
                .statusCode(204);
    }

    @Test
    void testMoveDataBetweenDatasources() {
        String camelsDbResult = RestAssured.given()
                .contentType(ContentType.TEXT).body("select * from camels order by id desc")
                .post("/test/execute")
                .then().extract().body().asString();

        RestAssured.given()
                .post("test/move-between-datasources");

        RestAssured.given()
                .contentType(ContentType.TEXT).body("select * from camelsProcessed order by id desc")
                .post("/test/execute")
                .then()
                .body(equalTo(camelsDbResult));
    }
}
