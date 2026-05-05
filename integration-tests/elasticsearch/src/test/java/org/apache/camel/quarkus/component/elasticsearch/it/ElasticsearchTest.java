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
package org.apache.camel.quarkus.component.elasticsearch.it;

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.Matchers.is;

@TestCertificates(certificates = {
        @Certificate(name = ElasticsearchTestResource.CERTIFICATE_NAME, formats = {
                Format.PKCS12 }, password = ElasticsearchTestResource.KEYSTORE_PASSWORD)
}, docker = true)
@QuarkusTest
@QuarkusTestResource(ElasticsearchTestResource.class)
class ElasticsearchTest {
    private static final Logger LOG = Logger.getLogger(ElasticsearchTest.class);

    @BeforeEach
    public void beforeEach() throws ConditionTimeoutException {
        // Ensure the Elasticsearch cluster is ready before each test
        waitClusterReady();
    }

    @AfterEach
    public void afterEach() {
        // Clean up all indexed data
        RestAssured.given()
                .queryParam("component", "elasticsearch")
                .queryParam("indexName", "_all")
                .delete("/elasticsearch/delete/index")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchBasicOperations(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "test-key";
        String indexValue = "Hello Camel Quarkus ElasticSearch";

        // Verify the ElasticSearch server is available
        RestAssured.given()
                .queryParam("component", component)
                .get("/elasticsearch/ping")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Index data
        String indexId = RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Retrieve indexed data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .queryParam("indexKey", indexKey)
                .get("/elasticsearch/get")
                .then()
                .statusCode(200)
                .body(is(indexValue));

        // Update indexed data
        String updatedIndexValue = indexValue + " Updated";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("component", component)
                .queryParam("indexId", indexId)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .body(updatedIndexValue)
                .patch("/elasticsearch/update")
                .then()
                .statusCode(200);

        // Verify updated data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .queryParam("indexKey", indexKey)
                .get("/elasticsearch/get")
                .then()
                .statusCode(200)
                .body(is(updatedIndexValue));

        // Delete indexed data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .delete("/elasticsearch/delete")
                .then()
                .statusCode(204);

        // Verify data deleted
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .queryParam("indexKey", indexKey)
                .get("/elasticsearch/get")
                .then()
                .statusCode(404);
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchBulk(String component) {
        String indexName = UUID.randomUUID().toString();

        String indexId = RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .post("/elasticsearch/bulk")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .queryParam("indexKey", "camel")
                .get("/elasticsearch/get")
                .then()
                .statusCode(200)
                .body(is("quarkus"));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchDeleteIndex(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "test-key";
        String indexValue = "Hello Camel Quarkus ElasticSearch";

        // Index data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Delete indexed data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .delete("/elasticsearch/delete/index")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchSearch(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "camel-quarkus";
        String indexValue = "Sub Atomic, Super Fast Camel Quarkus";

        // Index data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Search data
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            String searchResult = RestAssured.given()
                    .queryParam("component", component)
                    .queryParam("indexKey", indexKey)
                    .body("Super Fast")
                    .get("/elasticsearch/search")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            return searchResult.equals(indexValue);
        });
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchSearchJSON(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "camel-quarkus";
        String indexValue = "Sub Atomic, Super Fast Camel Quarkus";

        // Index data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Search data
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            String searchResult = RestAssured.given()
                    .queryParam("component", component)
                    .queryParam("indexKey", indexKey)
                    .body("Super Fast")
                    .get("/elasticsearch/search/json")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            return searchResult.equals(indexValue);
        });
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchMultiSearch(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "camel-quarkus";
        String indexValue = "Sub Atomic, Super Fast Camel Quarkus";

        // Index data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Search data
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(100, TimeUnit.SECONDS).until(() -> {
            String hits = RestAssured.given()
                    .queryParam("component", component)
                    .queryParam("indexName", indexName)
                    .queryParam("indexKey", indexKey)
                    .body("Sub Atomic,Super Fast,Nonsense")
                    .get("/elasticsearch/search/multi")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            return hits.equals("2");
        });
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchMultiGet(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "test-key";

        // Index multiple documents
        String indexId1 = RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body("Document 1")
                .post("/elasticsearch/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        String indexId2 = RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body("Document 2")
                .post("/elasticsearch/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // MultiGet both documents using JSON array
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .contentType(ContentType.JSON)
                .body(List.of(indexId1, indexId2))
                .post("/elasticsearch/multiget")
                .then()
                .statusCode(200)
                .body(is("2"));
    }

    /**
     * Queries the Elasticsearch cluster health status and waits until it's green or yellow.
     * Retries with Awaitility until the cluster is ready.
     *
     * @throws ConditionTimeoutException if the request fails after all retries
     */
    private void waitClusterReady() throws ConditionTimeoutException {
        String hostAddresses = ConfigProvider.getConfig().getValue("camel.component.elasticsearch.host-addresses",
                String.class);
        String username = ConfigProvider.getConfig().getValue("camel.component.elasticsearch.user", String.class);
        String password = ConfigProvider.getConfig().getValue("camel.component.elasticsearch.password", String.class);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        RestAssuredConfig config = RestAssured.config().sslConfig(
                                new SSLConfig().trustStore(Paths.get("target/certs/elasticsearch-keystore.p12").toFile(),
                                        ElasticsearchTestResource.KEYSTORE_PASSWORD));

                        String response = RestAssured.given()
                                .config(config)
                                .auth().preemptive().basic(username, password)
                                .when()
                                .get(String.format("https://%s/_cluster/health", hostAddresses))
                                .then()
                                .extract()
                                .body()
                                .asString();

                        // Check if cluster status is green or yellow
                        if (response.contains("\"status\":\"green\"") || response.contains("\"status\":\"yellow\"")) {
                            LOG.info("Cluster health is ready: " + response);
                            return response;
                        } else {
                            LOG.info("Cluster not ready yet, current status: " + response);
                            return null;
                        }
                    } catch (Exception e) {
                        LOG.info("Failed to query cluster health: " + e.getMessage() + ", retrying...");
                        return null;
                    }
                }, Objects::nonNull);
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchSearchWithBuilder(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "camel-quarkus";
        String indexValue = "Sub Atomic, Super Fast Camel Quarkus";

        // Index data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Search data using Builder API
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            String searchResult = RestAssured.given()
                    .queryParam("component", component)
                    .queryParam("indexKey", indexKey)
                    .body("Super Fast")
                    .get("/elasticsearch/search/builder")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            return searchResult.equals(indexValue);
        });
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchUpdateWithBuilder(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "test-key";
        String indexValue = "Hello Camel Quarkus ElasticSearch";

        // Index data
        String indexId = RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Update indexed data using Builder API
        String updatedIndexValue = indexValue + " Updated";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("component", component)
                .queryParam("indexId", indexId)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .body(updatedIndexValue)
                .patch("/elasticsearch/update/builder")
                .then()
                .statusCode(200);

        // Verify updated data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .queryParam("indexKey", indexKey)
                .get("/elasticsearch/get")
                .then()
                .statusCode(200)
                .body(is(updatedIndexValue));
    }

    /**
     * This method returns array of component names used in test routes.
     * It can be handy e.g. for testing quarkus managed elasticsearch client.
     *
     * @return Component name used in route.
     */
    @SuppressWarnings("unused")
    private static String[] componentNames() {
        return new String[] { "elasticsearch" };
    }
}
