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
package org.apache.camel.quarkus.component.elasticsearch.rest.client.it;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@TestCertificates(certificates = {
        @Certificate(name = ElasticsearchRestTestResource.CERTIFICATE_NAME, formats = {
                Format.PKCS12 }, password = ElasticsearchRestTestResource.KEYSTORE_PASSWORD)
}, docker = true)
@QuarkusTest
@QuarkusTestResource(ElasticsearchRestTestResource.class)
class ElasticsearchRestClientTest {
    private static final Logger LOG = Logger.getLogger(ElasticsearchRestClientTest.class);

    @BeforeEach
    public void beforeEach() throws ConditionTimeoutException {
        // Ensure the Elasticsearch cluster is ready before each test
        waitClusterReady();
    }

    @AfterEach
    public void afterEach() {
        // Clean up all indexed data
        RestAssured.given()
                .queryParam("indexName", "_all")
                .delete("/elasticsearch-rest-client/delete/index")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testElasticsearchBasicOperations() {
        String indexName = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        String documentValue = "Camel Quarkus ElasticSearch";
        Document document = new Document();
        document.setId(documentId);
        document.setValue(documentValue);

        RestAssured.given()
                .queryParam("indexName", indexName)
                .post("/elasticsearch-rest-client/index/create")
                .then()
                .statusCode(201)
                .body(is("true"));

        // Index data
        String indexId = RestAssured.given()
                .queryParam("indexName", indexName)
                .contentType(ContentType.JSON)
                .body(document)
                .post("/elasticsearch-rest-client/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .queryParam("indexName", indexName)
                    .queryParam("indexId", indexId)
                    .get("/elasticsearch-rest-client/get")
                    .then()
                    .statusCode(200)
                    .body(
                            "id", is(documentId),
                            "value", is(documentValue));
        });

        // Update indexed data
        String updatedDocumentValue = documentValue + " Updated";
        document.setValue(updatedDocumentValue);
        RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("indexId", indexId)
                .queryParam("indexName", indexName)
                .body(document)
                .post("/elasticsearch-rest-client/index")
                .then()
                .statusCode(200);

        // Verify updated data
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .queryParam("indexName", indexName)
                    .queryParam("indexId", indexId)
                    .get("/elasticsearch-rest-client/get")
                    .then()
                    .statusCode(200)
                    .body(
                            "id", is(documentId),
                            "value", is(updatedDocumentValue));
        });

        // Delete indexed data
        RestAssured.given()
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .delete("/elasticsearch-rest-client/delete")
                .then()
                .statusCode(204);

        // Verify data deleted
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .queryParam("indexName", indexName)
                    .queryParam("indexId", indexId)
                    .get("/elasticsearch-rest-client/get")
                    .then()
                    .statusCode(404);
        });
    }

    @Test
    public void testElasticsearchIndexSettings() {
        String indexName = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        String documentValue = "Camel Quarkus ElasticSearch";
        Document document = new Document();
        document.setId(documentId);
        document.setValue(documentValue);

        String indexSettings = "{\"settings\":{\"number_of_replicas\": 1,\"number_of_shards\": 3,\"analysis\": {},\"refresh_interval\": \"1s\"},\"mappings\":{\"dynamic\": false,\"properties\": {\"title\": {\"type\": \"text\", \"analyzer\": \"english\"}}}}";
        RestAssured.given()
                .queryParam("indexName", indexName)
                .body(indexSettings)
                .post("/elasticsearch-rest-client/index/create")
                .then()
                .statusCode(201)
                .body(is("true"));

        // Index data
        String indexId = RestAssured.given()
                .queryParam("indexName", indexName)
                .contentType(ContentType.JSON)
                .body(document)
                .post("/elasticsearch-rest-client/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .queryParam("indexName", indexName)
                    .queryParam("indexId", indexId)
                    .get("/elasticsearch-rest-client/get")
                    .then()
                    .statusCode(200)
                    .body(
                            "id", is(documentId),
                            "value", is(documentValue));
        });

        // Delete indexed data
        RestAssured.given()
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .delete("/elasticsearch-rest-client/delete")
                .then()
                .statusCode(204);

        // Verify data deleted
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .queryParam("indexName", indexName)
                    .queryParam("indexId", indexId)
                    .get("/elasticsearch-rest-client/get")
                    .then()
                    .statusCode(404);
        });
    }

    @Test
    public void testElasticsearchDeleteIndex() {
        String indexName = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        String documentValue = "Camel Quarkus ElasticSearch";
        Document document = new Document();
        document.setId(documentId);
        document.setValue(documentValue);

        // Create index
        RestAssured.given()
                .queryParam("indexName", indexName)
                .post("/elasticsearch-rest-client/index/create")
                .then()
                .statusCode(201)
                .body(is("true"));

        // Index data
        String indexId = RestAssured.given()
                .queryParam("indexName", indexName)
                .contentType(ContentType.JSON)
                .body(document)
                .post("/elasticsearch-rest-client/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .queryParam("indexName", indexName)
                    .queryParam("indexId", indexId)
                    .get("/elasticsearch-rest-client/get")
                    .then()
                    .statusCode(200)
                    .body(
                            "id", is(documentId),
                            "value", is(documentValue));
        });

        // Delete indexed data
        RestAssured.given()
                .queryParam("indexName", indexName)
                .delete("/elasticsearch-rest-client/delete/index")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Verify data deleted
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .queryParam("indexName", indexName)
                    .queryParam("indexId", indexId)
                    .get("/elasticsearch-rest-client/get")
                    .then()
                    .statusCode(404);
        });
    }

    @Test
    public void testElasticsearchSearch() {
        String indexName = UUID.randomUUID().toString();
        String documentIdA = UUID.randomUUID().toString();
        String documentValueA = "Camel Quarkus ElasticSearch A";
        Document documentA = new Document();
        documentA.setId(documentIdA);
        documentA.setValue(documentValueA);

        String documentIdB = UUID.randomUUID().toString();
        String documentValueB = "Camel Quarkus ElasticSearch B";
        Document documentB = new Document();
        documentB.setId(documentIdB);
        documentB.setValue(documentValueB);

        // Create index
        RestAssured.given()
                .queryParam("indexName", indexName)
                .post("/elasticsearch-rest-client/index/create")
                .then()
                .statusCode(201)
                .body(is("true"));

        // Index data
        RestAssured.given()
                .queryParam("indexName", indexName)
                .contentType(ContentType.JSON)
                .body(documentA)
                .post("/elasticsearch-rest-client/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        RestAssured.given()
                .queryParam("indexName", indexName)
                .contentType(ContentType.JSON)
                .body(documentB)
                .post("/elasticsearch-rest-client/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Search all data
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .queryParam("indexName", indexName)
                    .body(Map.of())
                    .get("/elasticsearch-rest-client/search")
                    .then()
                    .statusCode(200)
                    .body(
                            "id[0]", is(documentIdA),
                            "value[0]", is(documentValueA),
                            "id[1]", is(documentIdB),
                            "value[1]", is(documentValueB));
        });

        // Search specific data
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .queryParam("indexName", indexName)
                    .body(Map.of("id", documentIdB))
                    .get("/elasticsearch-rest-client/search")
                    .then()
                    .statusCode(200)
                    .body(
                            "id[0]", is(documentIdB),
                            "value[0]", is(documentValueB));
        });
    }

    @Test
    public void testElasticsearchSearchJson() {
        String indexName = UUID.randomUUID().toString();
        String documentIdA = UUID.randomUUID().toString();
        String documentValueA = "Camel Quarkus ElasticSearch A";
        Document documentA = new Document();
        documentA.setId(documentIdA);
        documentA.setValue(documentValueA);

        String documentIdB = UUID.randomUUID().toString();
        String documentValueB = "Camel Quarkus ElasticSearch B";
        Document documentB = new Document();
        documentB.setId(documentIdB);
        documentB.setValue(documentValueB);

        // Create index
        RestAssured.given()
                .queryParam("indexName", indexName)
                .post("/elasticsearch-rest-client/index/create")
                .then()
                .statusCode(201)
                .body(is("true"));

        // Index data
        RestAssured.given()
                .queryParam("indexName", indexName)
                .contentType(ContentType.JSON)
                .body(documentA)
                .post("/elasticsearch-rest-client/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        RestAssured.given()
                .queryParam("indexName", indexName)
                .contentType(ContentType.JSON)
                .body(documentB)
                .post("/elasticsearch-rest-client/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Search data
        String query = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"id\": \"" + documentIdB
                + "\"}},{\"match\": { \"value\": \"" + documentValueB + "\"}}]}}}";
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("indexName", indexName)
                    .body(query)
                    .get("/elasticsearch-rest-client/search")
                    .then()
                    .statusCode(200)
                    .body(
                            "id[0]", is(documentIdB),
                            "value[0]", is(documentValueB));
        });
    }

    /**
     * Queries the Elasticsearch cluster health status and waits until it's green or yellow.
     * Retries with Awaitility until the cluster is ready.
     *
     * @throws ConditionTimeoutException if the request fails after all retries
     */
    private void waitClusterReady() throws ConditionTimeoutException {
        String hostAddresses = ConfigProvider.getConfig()
                .getValue("camel.component.elasticsearch-rest-client.host-addresses-list", String.class);
        String username = ConfigProvider.getConfig()
                .getValue("camel.component.elasticsearch-rest-client.user", String.class);
        String password = ConfigProvider.getConfig()
                .getValue("camel.component.elasticsearch-rest-client.password", String.class);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        URL url = new URL(String.format("http://%s/_cluster/health", hostAddresses));
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                        // Set up Basic Authentication
                        String auth = String.format("%s:%s", username, password);
                        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(5000);
                        connection.setReadTimeout(5000);

                        int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(connection.getInputStream()))) {
                                StringBuilder response = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    response.append(line);
                                }
                                String healthJson = response.toString();

                                // Check if cluster status is green or yellow
                                if (healthJson.contains("\"status\":\"green\"")
                                        || healthJson.contains("\"status\":\"yellow\"")) {
                                    LOG.info("Cluster health is ready: " + healthJson);
                                    return healthJson;
                                } else {
                                    LOG.info("Cluster not ready yet, current status: " + healthJson);
                                    return null;
                                }
                            }
                        } else {
                            LOG.info("Cluster health check returned code: " + responseCode + ", retrying...");
                            return null;
                        }
                    } catch (Exception e) {
                        LOG.info("Failed to query cluster health: " + e.getMessage() + ", retrying...");
                        return null;
                    }
                }, Objects::nonNull);
    }
}
