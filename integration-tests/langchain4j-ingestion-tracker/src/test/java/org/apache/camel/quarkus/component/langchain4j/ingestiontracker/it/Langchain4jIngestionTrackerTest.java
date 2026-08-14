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
package org.apache.camel.quarkus.component.langchain4j.ingestiontracker.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Runs the {@code IngestionTracker} behavioural guarantees (mirrored from {@code JdbcIngestionTrackerTest})
 * against a real PostgreSQL server, provisioned by Quarkus Dev Services, through
 * {@link IngestionTrackerResource}.
 */
@QuarkusTest
class Langchain4jIngestionTrackerTest {

    @Test
    void unknownDocumentReadsEmpty() {
        RestAssured.get("/ingestion-tracker/p/missing").then().statusCode(404);
    }

    @Test
    void intentIsDurableAndNeverSkippable() {
        RestAssured.given()
                .queryParam("fingerprint", "fp1").queryParam("contentHash", "hash1")
                .queryParam("committedCount", 0).queryParam("intendedCount", 5).queryParam("origin", "source")
                .post("/ingestion-tracker/p/doc/intent")
                .then().statusCode(200)
                .body("status", equalTo("in_progress"))
                .body("intendedCount", equalTo(5));
    }

    @Test
    void commitCompletesTheIntent() {
        writeIntent("p", "doc", "fp1", "hash1", 0, 5, "source");

        RestAssured.given()
                .queryParam("fingerprint", "fp1").queryParam("contentHash", "hash1").queryParam("segmentCount", 3)
                .post("/ingestion-tracker/p/doc/commit")
                .then().statusCode(200)
                .body("status", equalTo("done"))
                .body("fingerprint", equalTo("fp1"))
                .body("contentHash", equalTo("hash1"))
                .body("segmentCount", equalTo(3));
    }

    @Test
    void reintentKeepsTheLargestKnownCount() {
        writeIntent("p", "doc", "fp1", "hash1", 0, 5, "source");
        commit("p", "doc", "fp1", "hash1", 5);
        writeIntent("p", "doc", "fp2", "hash2", 5, 2, "source");

        // shrink must still see the previously committed tail
        RestAssured.get("/ingestion-tracker/p/doc").then()
                .body("segmentCount", equalTo(5));
    }

    @Test
    void refreshFingerprintTouchesNothingElse() {
        writeIntent("p", "doc", "fp1", "hash1", 0, 2, "source");
        commit("p", "doc", "fp1", "hash1", 2);

        RestAssured.given().queryParam("fingerprint", "fp2")
                .post("/ingestion-tracker/p/doc/refresh-fingerprint")
                .then().statusCode(200)
                .body("fingerprint", equalTo("fp2"))
                .body("contentHash", equalTo("hash1"))
                .body("segmentCount", equalTo(2))
                .body("status", equalTo("done"));
    }

    @Test
    void listDocumentsIsIsolatedByPipeline() {
        writeIntent("p1", "a", "fp", "h", 0, 1, "source");
        commit("p1", "a", "fp", "h", 1);
        writeIntent("p2", "b", "fp", "h", 0, 1, "source");
        commit("p2", "b", "fp", "h", 1);

        RestAssured.get("/ingestion-tracker/p1").then()
                .body("", hasSize(1))
                .body("[0].documentId", equalTo("a"));
    }

    @Test
    void deleteRowForgetsTheDocument() {
        writeIntent("p", "doc", "fp", "h", 0, 1, "source");
        commit("p", "doc", "fp", "h", 1);

        RestAssured.delete("/ingestion-tracker/p/doc").then().statusCode(204);

        RestAssured.get("/ingestion-tracker/p/doc").then().statusCode(404);
        RestAssured.get("/ingestion-tracker/p").then().body("", hasSize(0));
    }

    @Test
    void tombstoneSurvivesAndLifts() {
        writeIntent("p", "doc", "fp", "h", 0, 1, "source");
        commit("p", "doc", "fp", "h", 1);

        RestAssured.post("/ingestion-tracker/p/doc/tombstone").then().body("tombstone", equalTo(true));
        RestAssured.post("/ingestion-tracker/p/doc/unsuppress").then().body("tombstone", equalTo(false));
    }

    @Test
    void pinSurvivesAndLifts() {
        writeIntent("p", "doc", "fp", "h", 0, 1, "api");
        commit("p", "doc", "fp", "h", 1);

        RestAssured.post("/ingestion-tracker/p/doc/pin").then().body("pinned", equalTo(true));
        RestAssured.post("/ingestion-tracker/p/doc/unpin").then().body("pinned", equalTo(false));
    }

    @Test
    void markFailedRecordsTheAttemptAndKeepsCommittedState() {
        writeIntent("p", "doc", "fp1", "h1", 0, 2, "source");
        commit("p", "doc", "fp1", "h1", 2);

        RestAssured.given().queryParam("fingerprint", "fp2")
                .post("/ingestion-tracker/p/doc/fail")
                .then().statusCode(200)
                .body("status", equalTo("failed"))
                // the failed attempt's fingerprint gates retries
                .body("fingerprint", equalTo("fp2"))
                // previously committed segments keep serving
                .body("segmentCount", equalTo(2));
    }

    private static void writeIntent(String pipeline, String docId, String fingerprint, String contentHash,
            int committedCount, int intendedCount, String origin) {
        RestAssured.given()
                .queryParam("fingerprint", fingerprint).queryParam("contentHash", contentHash)
                .queryParam("committedCount", committedCount).queryParam("intendedCount", intendedCount)
                .queryParam("origin", origin)
                .post("/ingestion-tracker/" + pipeline + "/" + docId + "/intent")
                .then().statusCode(200);
    }

    private static void commit(String pipeline, String docId, String fingerprint, String contentHash,
            int segmentCount) {
        RestAssured.given()
                .queryParam("fingerprint", fingerprint).queryParam("contentHash", contentHash)
                .queryParam("segmentCount", segmentCount)
                .post("/ingestion-tracker/" + pipeline + "/" + docId + "/commit")
                .then().statusCode(200);
    }
}
