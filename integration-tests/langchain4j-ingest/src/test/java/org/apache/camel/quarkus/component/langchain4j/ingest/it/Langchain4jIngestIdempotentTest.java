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
package org.apache.camel.quarkus.component.langchain4j.ingest.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The idempotent repository: an edited file re-ingests under a new key (path, mtime, size) with the
 * previous segments kept, and a consumer-fed pipeline deduplicates by document id, first write
 * wins.
 */
@QuarkusTest
class Langchain4jIngestIdempotentTest {

    @Test
    void editedFileReIngestsAndAppends() {
        Langchain4jIngestTest.write("versioned.txt", "The RHO-1 relay ships as rev ALPHA.");
        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertNotNull(
                        Langchain4jIngestTest.hit("Which rev ships?", null, "rev ALPHA"),
                        "the first version must be ingested"));

        // different length guarantees a new key even within the mtime granularity
        Langchain4jIngestTest.write("versioned.txt", "The RHO-1 relay now ships as rev BRAVO, improved.");
        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertNotNull(
                        Langchain4jIngestTest.hit("Which rev ships?", null, "rev BRAVO"),
                        "the edited version must be re-ingested under its new key"));

        // append mode: the first version's segments were not replaced
        assertNotNull(Langchain4jIngestTest.hit("Which rev ships?", null, "rev ALPHA"),
                "append mode keeps the previous version's segments");
    }

    @Test
    void duplicateDocumentIdIsSkipped() {
        String first = RestAssured.given().contentType(ContentType.TEXT)
                .body("The KAPPA-4 sensor reads humidity.")
                .post("/langchain4j-ingest/feed/custom/dedup/kappa.txt")
                .then().statusCode(200).extract().asString();
        assertEquals("ingested", first);

        // same document id again: the register skips it, first write wins
        String second = RestAssured.given().contentType(ContentType.TEXT)
                .body("The KAPPA-4 sensor allegedly reads pressure now.")
                .post("/langchain4j-ingest/feed/custom/dedup/kappa.txt")
                .then().statusCode(200).extract().asString();
        assertEquals("skipped", second);

        // the store holds the first write only, and the register committed the key
        assertNotNull(Langchain4jIngestTest.hit("What does the sensor read?", "custom", "humidity"));
        assertTrue(Langchain4jIngestTest.hits("What does the sensor read?", "custom").stream()
                .noneMatch(hit -> hit.get("text").contains("pressure")),
                "the duplicate delivery must not have been ingested");
        String contains = RestAssured.given()
                .queryParam("repo", "test-register")
                .queryParam("key", "dedup/kappa.txt")
                .get("/langchain4j-ingest/register-contains")
                .then().statusCode(200).extract().asString();
        assertEquals("true", contains);
    }

    /** A failed delivery releases the key ({@code removeOnFailure}), so the same id can retry. */
    @Test
    void failedDeliveryReleasesTheKey() {
        RestAssured.given().contentType(ContentType.TEXT)
                .body("The SIGMA-9 valve " + DeterministicEmbeddingModel.POISON + " fails to embed.")
                .post("/langchain4j-ingest/feed/custom/release/sigma9.txt")
                .then().statusCode(500);

        String retry = RestAssured.given().contentType(ContentType.TEXT)
                .body("The SIGMA-9 valve seals reliably.")
                .post("/langchain4j-ingest/feed/custom/release/sigma9.txt")
                .then().statusCode(200).extract().asString();
        assertEquals("ingested", retry);
    }

    /** A JDBC register (H2-backed {@code JdbcMessageIdRepository}): dedup through real SQL. */
    @Test
    void jdbcRegisterDeduplicates() {
        String first = RestAssured.given().contentType(ContentType.TEXT)
                .body("The THETA-2 pump moves coolant.")
                .post("/langchain4j-ingest/feed/jdbcdocs/dedup/theta.txt")
                .then().statusCode(200).extract().asString();
        assertEquals("ingested", first);

        String second = RestAssured.given().contentType(ContentType.TEXT)
                .body("The THETA-2 pump allegedly moves lava now.")
                .post("/langchain4j-ingest/feed/jdbcdocs/dedup/theta.txt")
                .then().statusCode(200).extract().asString();
        assertEquals("skipped", second);

        // contains() issues a SELECT against the H2 store, proving the key survived in SQL
        String contains = RestAssured.given()
                .queryParam("repo", "jdbcRegister")
                .queryParam("key", "dedup/theta.txt")
                .get("/langchain4j-ingest/register-contains")
                .then().statusCode(200).extract().asString();
        assertEquals("true", contains);
    }

    /**
     * The {@code datasheets} builder pipeline names {@code datasheetsRegister} with auto-create and no
     * bean defined anywhere: the auto-created register deduplicates too.
     */
    @Test
    void autoCreatedRegisterDeduplicates() {
        String first = RestAssured.given().contentType(ContentType.TEXT)
                .body("The OMICRON-8 filter removes particles.")
                .post("/langchain4j-ingest/feed/datasheets/dedup/omicron.txt")
                .then().statusCode(200).extract().asString();
        assertEquals("ingested", first);

        String second = RestAssured.given().contentType(ContentType.TEXT)
                .body("The OMICRON-8 filter allegedly removes odors now.")
                .post("/langchain4j-ingest/feed/datasheets/dedup/omicron.txt")
                .then().statusCode(200).extract().asString();
        assertEquals("skipped", second);

        String contains = RestAssured.given()
                .queryParam("repo", "datasheetsRegister")
                .queryParam("key", "dedup/omicron.txt")
                .get("/langchain4j-ingest/register-contains")
                .then().statusCode(200).extract().asString();
        assertEquals("true", contains);
    }
}
