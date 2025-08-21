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
package org.apache.camel.quarkus.component.sql.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(SqlMultipleNamedDatasourceWithNoDefaultTest.class)
public class SqlMultipleNamedDatasourceWithNoDefaultTest implements QuarkusTestProfile {
    @Test
    void multipleNamedDataSourcesWithoutDefaultNotAutowirable() {
        // There are multiple named DataSource beans but no default bean
        // DataSource autowiring should fail
        RestAssured.given()
                .get("/sql/datasource")
                .then()
                .statusCode(500)
                .body(endsWith("Property 'dataSource' is required"));
    }

    @Test
    void multipleNamedDataSourcesWithExplicitLookup() {
        RestAssured.given()
                .queryParam("dataSourceRef", "testB")
                .get("/sql/datasource")
                .then()
                .statusCode(200)
                .body(
                        "name", is("testB"),
                        "default", is(false));
    }

    @Override
    public String getConfigProfile() {
        return "multi-ds-no-default";
    }
}
