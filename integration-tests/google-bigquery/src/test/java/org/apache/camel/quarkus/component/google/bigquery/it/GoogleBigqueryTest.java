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
package org.apache.camel.quarkus.component.google.bigquery.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_APPLICATION_CREDENTIALS", matches = ".+")
class GoogleBigqueryTest {

    //@Test
    public void googleBigQueryCrudOperations() {
        try {
            // Create table
            RestAssured.post("/google-bigquery/table")
                    .then()
                    .statusCode(201);

            // Insert rows
            for (int i = 1; i <= 3; i++) {
                Map<String, String> object = new HashMap<>();
                object.put("id", String.valueOf(i));
                object.put("col1", String.valueOf(i + 1));
                object.put("col2", String.valueOf(i + 2));

                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(object)
                        .post("/google-bigquery")
                        .then()
                        .statusCode(201);
            }

            // Verify rows exits
            RestAssured.get("/google-bigquery")
                    .then()
                    .statusCode(200)
                    .body(is("3"));

            // Verify rows exits using a query resource from the filesystem
            RestAssured.get("/google-bigquery/file")
                    .then()
                    .statusCode(200)
                    .body(is("3"));
        } finally {
            // Delete table
            RestAssured.delete("/google-bigquery/table")
                    .then()
                    .statusCode(200);
        }
    }
}
