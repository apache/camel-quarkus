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

import java.io.FileInputStream;
import java.util.Locale;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.cloud.bigquery.TimePartitioning;
import org.apache.camel.quarkus.test.support.google.GoogleCloudContext;
import org.apache.camel.quarkus.test.support.google.GoogleCloudTestResource;
import org.apache.camel.quarkus.test.support.google.GoogleTestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.GenericContainer;

public class GoogleBigqueryCustomizer implements GoogleTestEnvCustomizer {

    private static final String TEST_PROJECT_ID = "test-project";
    private static final String DOCKER_IMAGE_WIREMOCK = "wiremock/wiremock:2.33.2";

    private GenericContainer container;

    @Override
    public GenericContainer createContainer() {
        return null;
    }

    @Override
    public void customize(GoogleCloudContext envContext) {

        try {
            TestMode mode = detectMode(envContext);
            envContext.property("google-bigquery.testMode", mode.name());

            String projectId = envContext.getProperties().getOrDefault(GoogleCloudTestResource.PARAM_PROJECT_ID,
                    TEST_PROJECT_ID);
            envContext.property("project.id", projectId);

            final boolean generateSuffixes = mode == TestMode.realService;

            // ------------------ generate names ----------------
            final String datasetName = generateName("test_dataset", envContext, generateSuffixes);
            final String tableNameForMap = generateName("table_for_map", envContext, generateSuffixes);
            final String tableNameForList = generateName("table_for_list", envContext, generateSuffixes);
            final String tableNameForTemplate = generateName("table_for_template", envContext, generateSuffixes);
            final String tableNameForPartitioning = generateName("table_for_partitioning", envContext, generateSuffixes);
            final String tableNameForInsertId = generateName("table_for_insert-id", envContext, generateSuffixes);
            final String tableNameForSqlCrud = generateName("table_for_sql_crud", envContext, generateSuffixes);

            if (mode == TestMode.mockedBacked) {
                //do not create resources in mocked backend
                return;
            }

            final BigQuery bigQuery = getClient(projectId,
                    envContext.getProperties().get(GoogleCloudTestResource.PARAM_CREDENTIALS_PATH));

            // --------------- create ------------------------
            bigQuery.create(DatasetInfo.newBuilder(datasetName).build());

            final Schema schema = Schema.of(
                    Field.of("id", StandardSQLTypeName.NUMERIC),
                    Field.of("col1", StandardSQLTypeName.STRING),
                    Field.of("col2", StandardSQLTypeName.STRING));
            createTable(bigQuery, datasetName, tableNameForMap, schema, null);
            createTable(bigQuery, datasetName, tableNameForList, schema, null);
            createTable(bigQuery, datasetName, tableNameForTemplate, schema, null);
            createTable(bigQuery, datasetName, tableNameForInsertId, schema, null);

            //Numeric types can not be used as a headers parameters - see https://issues.apache.org/jira/browse/CAMEL-18382
            //new schema uses all columns of string type
            final Schema sqlSchema = Schema.of(
                    Field.of("id", StandardSQLTypeName.NUMERIC),
                    Field.of("col1", StandardSQLTypeName.NUMERIC),
                    Field.of("col2", StandardSQLTypeName.NUMERIC));
            createTable(bigQuery, datasetName, tableNameForSqlCrud, sqlSchema, null);

            final Schema partitioningSchema = Schema.of(
                    Field.of("id", StandardSQLTypeName.NUMERIC),
                    Field.of("col1", StandardSQLTypeName.STRING),
                    Field.of("col2", StandardSQLTypeName.STRING),
                    Field.of("date", StandardSQLTypeName.DATE));
            createTable(bigQuery, datasetName, tableNameForPartitioning, partitioningSchema,
                    TimePartitioning.newBuilder(TimePartitioning.Type.DAY)
                            .setField("date") //  name of column to use for partitioning
                            .setExpirationMs(7776000000L) // 90 days
                            .build());

            // --------------- delete ------------------------
            envContext.closeable(() -> {
                bigQuery.delete(TableId.of(datasetName, tableNameForMap));
                bigQuery.delete(TableId.of(datasetName, tableNameForList));
                bigQuery.delete(TableId.of(datasetName, tableNameForTemplate));
                bigQuery.delete(TableId.of(datasetName, tableNameForTemplate + "_suffix"));
                bigQuery.delete(TableId.of(datasetName, tableNameForPartitioning));
                bigQuery.delete(TableId.of(datasetName, tableNameForInsertId));
                bigQuery.delete(TableId.of(datasetName, tableNameForSqlCrud));

                bigQuery.delete(TableId.of(datasetName, tableNameForPartitioning + "$decorator"));

                bigQuery.delete(DatasetId.of(projectId, datasetName));

            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateName(String name, GoogleCloudContext envContext, boolean generateSuffixes) {
        String retVal = "google_bigquery_" + name;
        if (generateSuffixes) {
            retVal = retVal + "_" + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
        }
        envContext.property("google-bigquery." + name, retVal);
        return retVal;
    }

    public static void createTable(BigQuery bigQuery, String datasetName, String tableName, Schema schema,
            TimePartitioning timePartitioning) {
        try {
            // Initialize client that will be used to send requests. This client only needs to be created
            // once, and can be reused for multiple requests.
            TableId tableId = TableId.of(datasetName, tableName);
            TableDefinition tableDefinition;
            if (timePartitioning == null) {
                tableDefinition = StandardTableDefinition.of(schema);
            } else {
                tableDefinition = StandardTableDefinition.newBuilder()
                        .setSchema(schema)
                        .setTimePartitioning(timePartitioning)
                        .build();
            }
            TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();

            bigQuery.create(tableInfo);
        } catch (BigQueryException e) {
            throw new RuntimeException(e);
        }
    }

    static BigQuery getClient(String projectId, String credentialsPath) throws Exception {

        // Load credentials from JSON key file. If you can't set the GOOGLE_APPLICATION_CREDENTIALS
        // environment variable, you can explicitly load the credentials file to construct the
        // credentials.
        GoogleCredentials credentials;
        try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
            credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
        }

        // Instantiate a client.
        return BigQueryOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build()
                .getService();
    }

    enum TestMode {
        realService, mockedBacked, wiremockRecording;
    }

    private TestMode detectMode(GoogleCloudContext envContext) {
        boolean isUsingMockBackend = envContext.isUsingMockBackend();

        String recordEnabled = System.getProperty("wiremock.record", System.getenv("WIREMOCK_RECORD"));
        boolean isRecordingEnabled = recordEnabled != null && recordEnabled.equals("true");

        if (isRecordingEnabled) {
            if (isUsingMockBackend) {
                throw new IllegalStateException(
                        "For Wiremock recording real account has to be provided! Set GOOGLE_APPLICATION_CREDENTIALS and GOOGLE_PROJECT_ID env vars.");
            }
            return TestMode.wiremockRecording;
        }

        if (isUsingMockBackend) {
            return TestMode.mockedBacked;
        }

        return TestMode.realService;
    }

}
