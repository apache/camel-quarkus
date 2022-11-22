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

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.google.bigquery.GoogleBigQueryConstants;
import org.apache.camel.quarkus.test.support.google.GoogleCloudTestResource;
import org.apache.camel.quarkus.test.support.google.GoogleProperty;
import org.apache.camel.util.CollectionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.apache.camel.util.CollectionHelper.mergeMaps;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestHTTPEndpoint(GoogleBigqueryResource.class)
@QuarkusTestResource(GoogleBigqueryWiremockTestResource.class)
@QuarkusTestResource(GoogleCloudTestResource.class)
class GoogleBigqueryTest {

    @GoogleProperty(name = "project.id")
    String projectId;

    @GoogleProperty(name = "google.credentialsPath")
    String credentialsPath;

    @GoogleProperty(name = "google-bigquery.test_dataset")
    String dataset;

    @GoogleProperty(name = "google-bigquery.table_for_map")
    String tableNameForMap;

    @GoogleProperty(name = "google-bigquery.table_for_list")
    String tableNameForList;

    @GoogleProperty(name = "google-bigquery.table_for_template")
    String tableNameForTemplate;

    @GoogleProperty(name = "google-bigquery.table_for_partitioning")
    String tableNameForPartitioning;

    @GoogleProperty(name = "google-bigquery.table_for_insert-id")
    String tableNameForInsertId;

    @GoogleProperty(name = "google-bigquery.table_for_sql_crud")
    String tableNameForSqlCrud;

    @GoogleProperty(name = "google-bigquery.testMode")
    String testMode;

    @Test
    public void insertMapTest() throws Exception {
        // Insert rows
        for (int i = 1; i <= 3; i++) {
            Map<String, String> object = new HashMap<>();
            object.put("id", String.valueOf(i));
            object.put("col1", String.valueOf(i + 1));
            object.put("col2", String.valueOf(i + 2));

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(object)
                    .queryParam("tableName", tableNameForMap)
                    .post("insertMap")
                    .then()
                    .statusCode(201);
        }

        if (isMockBackend()) {
            //no assertion is required, in case of an error, the request won't be found
            return;
        }

        TableResult tr = getTableData(dataset + "." + tableNameForMap);
        Assertions.assertEquals(3, tr.getTotalRows());
    }

    @Test
    public void insertListTest() throws Exception {
        // Insert rows
        List data = new LinkedList();
        for (int i = 1; i <= 3; i++) {
            Map<String, String> object = new HashMap<>();
            object.put("id", String.valueOf(i));
            object.put("col1", String.valueOf(i + 1));
            object.put("col2", String.valueOf(i + 2));

            data.add(object);
        }

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(data)
                .queryParam("tableName", tableNameForList)
                .post("insertList")
                .then()
                .statusCode(201);

        if (isMockBackend()) {
            //no assertion is required, in case of an error, the request won't be found
            return;
        }

        TableResult tr = getTableData(dataset + "." + tableNameForList);
        Assertions.assertEquals(3, tr.getTotalRows());
    }

    @Test
    public void templateTableTest() throws Exception {
        // Insert rows
        for (int i = 1; i <= 5; i++) {
            Map<String, String> object = new HashMap<>();
            object.put("id", String.valueOf(i));
            object.put("col1", String.valueOf(i + 1));
            object.put("col2", String.valueOf(i + 2));

            if (i <= 3) {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(object)
                        .queryParam("tableName", tableNameForTemplate)
                        .post("insertMap")
                        .then()
                        .statusCode(201);
            } else {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(object)
                        .queryParam("tableName", tableNameForTemplate)
                        .queryParam("headerKey", GoogleBigQueryConstants.TABLE_SUFFIX)
                        .queryParam("headerValue", "_suffix")
                        .post("insertMap")
                        .then()
                        .statusCode(201);
            }
        }

        if (isMockBackend()) {
            //no assertion is required, in case of an error, the request won't be found
            return;
        }

        TableResult tr = getTableData(dataset + "." + tableNameForTemplate);
        Assertions.assertEquals(3, tr.getTotalRows());
        tr = getTableData(dataset + "." + tableNameForTemplate + "_suffix");
        Assertions.assertEquals(2, tr.getTotalRows());
    }

    @Test
    public void partitioningTest() throws Exception {
        //it is not possible to test GoogleBigQueryConstants.PARTITION_DECORATOR, because of the error:
        //`Streaming to metadata partition of column based partitioning table * is disallowed'
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Insert rows
        for (int i = 1; i <= 2; i++) {
            Map<String, String> object = new HashMap<>();
            object.put("id", String.valueOf(i));
            object.put("col1", String.valueOf(i + 1));
            object.put("col2", String.valueOf(i + 2));
            //data has to be long in milliseconds (Resource convets value into `com.google.api.client.util.DateTime`)
            //use value of current day + i
            LocalDateTime day = isMockBackend() ? LocalDateTime.of(2022, 8, 22 + i, 0, 0)
                    : ZonedDateTime.now().toLocalDate().plusDays(i).atStartOfDay();
            object.put("date", day.format(formatter));

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(object)
                    .queryParam("tableName", tableNameForPartitioning)
                    .post("insertMap")
                    .then()
                    .statusCode(201);
        }

        if (isMockBackend()) {
            //no assertion is required, in case of an error, the request won't be found
            return;
        }

        TableResult tr = getTableData(dataset + "." + tableNameForPartitioning);
        Assertions.assertEquals(2, tr.getTotalRows());
    }

    @Test
    public void insertIdTest() throws Exception {
        // Insert rows
        for (int i = 1; i <= 2; i++) {
            Map<String, String> object = new HashMap<>();
            object.put("id", "1");
            object.put("col1", String.valueOf(i + 1));
            object.put("col2", String.valueOf(i + 2));

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(object)
                    .queryParam("tableName", tableNameForInsertId)
                    .queryParam("headerKey", GoogleBigQueryConstants.INSERT_ID)
                    .queryParam("headerValue", "id")
                    .post("insertMap")
                    .then()
                    .statusCode(201);
        }

        if (isMockBackend()) {
            //no assertion is required, in case of an error, the request won't be found
            return;
        }

        TableResult tr = getTableData(dataset + "." + tableNameForInsertId);
        Assertions.assertEquals(1, tr.getTotalRows());
    }

    private TableResult getTableData(String tableId) throws Exception {
        QueryJobConfiguration queryConfig = QueryJobConfiguration
                .newBuilder(String.format("SELECT * FROM `%s` ORDER BY id", tableId))
                .setUseLegacySql(false)
                .build();

        BigQuery client = GoogleBigqueryCustomizer.getClient(projectId, credentialsPath);
        Job queryJob = client.create(JobInfo.newBuilder(queryConfig).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            // You can also look at queryJob.getStatus().getExecutionErrors() for all
            // errors, not just the latest one.
            throw new RuntimeException(queryJob.getStatus().getError().toString());
        }

        return queryJob.getQueryResults();
    }

    private List<List<Object>> parseResult(TableResult tr) {
        List<List<Object>> retVal = new ArrayList<>();
        for (FieldValueList flv : tr.getValues()) {
            retVal.add(flv.stream().map(fv -> fv.getValue()).collect(Collectors.toList()));
        }
        return retVal;
    }

    @Test
    public void sqlCrudOperations() throws Exception {
        // create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(mapWithJobId("job01", mapOf("id", 1, "col1", 2, "col2", 3)))
                .queryParam("sql", String.format("INSERT INTO `%s.%s.%s` VALUES(@id, @col1, @col2)",
                        projectId, dataset, tableNameForSqlCrud))
                .queryParam("body", "col1")
                .queryParam("file", true)
                .post("executeSql")
                .then()
                .statusCode(200)
                .body(is("1"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                //once test is disabled, make sure that the types of the columns are aligned to the schema
                //see https://issues.apache.org/jira/browse/CAMEL-18437 for more details
                .body(mapWithJobId("job02", mapOf("id", 2, "col1", 3, "col2", 4)))
                .queryParam("sql", String.format("INSERT INTO `%s.%s.%s` VALUES(@id, ${col1}, ${col2})",
                        projectId, dataset, tableNameForSqlCrud))
                .post("executeSql")
                .then()
                .statusCode(200)
                .body(is("1"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(mapWithJobId("job03", Collections.emptyMap()))
                .queryParam("file", true)
                .queryParam("sql", String.format("SELECT * FROM `%s.%s.%s`",
                        projectId, dataset, tableNameForSqlCrud))
                .post("executeSql")
                .then()
                .statusCode(200)
                .body(is("2"));

        //update
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(mapWithJobId("job04", CollectionHelper.mapOf("col1", 22, "id", 1)))
                .queryParam("sql", String.format("UPDATE `%s.%s.%s` SET col1=@col1 WHERE id=@id",
                        projectId, dataset, tableNameForSqlCrud))
                .post("executeSql")
                .then()
                .statusCode(200)
                .body(is("1"));

        //no assertion is required for mock backend, if there is a problem with request, wiremock would fail
        if (!isMockBackend()) {
            TableResult tr = getTableData(dataset + "." + tableNameForSqlCrud);
            Assertions.assertEquals(2, tr.getTotalRows());
            List<List<Object>> results = parseResult(tr);
            Assertions.assertEquals("22", results.get(0).get(1));
        }

        //delete
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(mapWithJobId("job05", Collections.emptyMap()))
                .queryParam("sql", String.format("DELETE FROM `%s.%s.%s` WHERE id=1",
                        projectId, dataset, tableNameForSqlCrud))
                .post("executeSql")
                .then()
                .statusCode(200)
                .body(is("1"));

        if (isMockBackend()) {
            //no assertion is required, if there is a problem with request, wiremock would fail
            return;
        }

        TableResult tr = getTableData(dataset + "." + tableNameForSqlCrud);
        Assertions.assertEquals(1, tr.getTotalRows());
        List<List<Object>> results = parseResult(tr);
        Assertions.assertEquals("3", results.get(0).get(1));

    }

    private boolean isMockBackend() {
        return GoogleBigqueryCustomizer.TestMode.valueOf(testMode) == GoogleBigqueryCustomizer.TestMode.mockedBacked;
    }

    private Map<String, Object> mapWithJobId(String jobId, Map<String, Object> map) {
        if (isMockBackend()) {
            return mergeMaps(Collections.singletonMap(GoogleBigQueryConstants.JOB_ID, jobId), map);
        }

        return map;
    }
}
