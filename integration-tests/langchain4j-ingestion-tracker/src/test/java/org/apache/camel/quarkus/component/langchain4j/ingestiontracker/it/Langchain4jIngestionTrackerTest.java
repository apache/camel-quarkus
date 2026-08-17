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
 * {@link IngestionTrackerResource}. Each test uses its own pipeline id — the database is shared
 * across methods and never reset, so isolation comes from the keys.
 */
@QuarkusTest
class Langchain4jIngestionTrackerTest {

    @Test
    void unknownDocumentReadsEmpty() {
        RestAssured.get("/ingestion-tracker/p-unknown/missing").then().statusCode(404);
    }

    @Test
    void intentIsDurableAndNeverSkippable() {
        RestAssured.given()
                .queryParam("fingerprint", "fp1").queryParam("contentHash", "hash1")
                .queryParam("intendedCount", 5).queryParam("origin", "source")
                .post("/ingestion-tracker/p-intent/doc/intent")
                .then().statusCode(200)
                .body("status", equalTo("in_progress"))
                .body("intendedCount", equalTo(5));
    }

    @Test
    void commitCompletesTheIntent() {
        writeIntent("p-commit", "doc", "fp1", "hash1", 5, "source");

        RestAssured.given()
                .queryParam("fingerprint", "fp1").queryParam("contentHash", "hash1").queryParam("segmentCount", 3)
                .post("/ingestion-tracker/p-commit/doc/commit")
                .then().statusCode(200)
                .body("status", equalTo("done"))
                .body("fingerprint", equalTo("fp1"))
                .body("contentHash", equalTo("hash1"))
                .body("segmentCount", equalTo(3));
    }

    @Test
    void commitWithoutIntentFails() {
        RestAssured.given()
                .queryParam("fingerprint", "fp").queryParam("contentHash", "h").queryParam("segmentCount", 1)
                .post("/ingestion-tracker/p-commit-ghost/ghost/commit")
                .then().statusCode(500);
    }

    @Test
    void reintentKeepsTheLargestKnownCount() {
        writeIntent("p-reintent", "doc", "fp1", "hash1", 5, "source");
        commit("p-reintent", "doc", "fp1", "hash1", 5);
        writeIntent("p-reintent", "doc", "fp2", "hash2", 2, "source");

        // shrink must still see the previously committed tail
        RestAssured.get("/ingestion-tracker/p-reintent/doc").then()
                .body("segmentCount", equalTo(5));
    }

    @Test
    void reintentBeforeCommitKeepsTheLargestIntendedCount() {
        writeIntent("p-reintent-crash", "doc", "fp1", "hash1", 5, "source");
        // no commit — the crashed attempt may have written up to 5 segments
        writeIntent("p-reintent-crash", "doc", "fp2", "hash2", 2, "source");

        RestAssured.get("/ingestion-tracker/p-reintent-crash/doc").then()
                .body("intendedCount", equalTo(5));
    }

    @Test
    void refreshFingerprintTouchesNothingElse() {
        writeIntent("p-refresh", "doc", "fp1", "hash1", 2, "source");
        commit("p-refresh", "doc", "fp1", "hash1", 2);

        RestAssured.given().queryParam("fingerprint", "fp2")
                .post("/ingestion-tracker/p-refresh/doc/refresh-fingerprint")
                .then().statusCode(200)
                .body("fingerprint", equalTo("fp2"))
                .body("contentHash", equalTo("hash1"))
                .body("segmentCount", equalTo(2))
                .body("status", equalTo("done"));
    }

    @Test
    void listDocumentsIsIsolatedByPipeline() {
        writeIntent("p-list-a", "a", "fp", "h", 1, "source");
        commit("p-list-a", "a", "fp", "h", 1);
        writeIntent("p-list-b", "b", "fp", "h", 1, "source");
        commit("p-list-b", "b", "fp", "h", 1);

        RestAssured.get("/ingestion-tracker/p-list-a").then()
                .body("", hasSize(1))
                .body("[0].documentId", equalTo("a"));
    }

    @Test
    void deleteRowForgetsTheDocument() {
        writeIntent("p-delete", "doc", "fp", "h", 1, "source");
        commit("p-delete", "doc", "fp", "h", 1);

        RestAssured.delete("/ingestion-tracker/p-delete/doc").then().statusCode(204);

        RestAssured.get("/ingestion-tracker/p-delete/doc").then().statusCode(404);
        RestAssured.get("/ingestion-tracker/p-delete").then().body("", hasSize(0));
    }

    @Test
    void tombstoneSurvivesAndLifts() {
        writeIntent("p-tombstone", "doc", "fp", "h", 1, "source");
        commit("p-tombstone", "doc", "fp", "h", 1);

        RestAssured.post("/ingestion-tracker/p-tombstone/doc/tombstone").then().body("tombstone", equalTo(true));
        RestAssured.post("/ingestion-tracker/p-tombstone/doc/unsuppress").then().body("tombstone", equalTo(false));
    }

    @Test
    void tombstoneOfANeverIngestedDocumentCreatesTheSuppressionRow() {
        RestAssured.post("/ingestion-tracker/p-tombstone-ghost/ghost/tombstone")
                .then().statusCode(200)
                .body("tombstone", equalTo(true))
                .body("status", equalTo("done"))
                .body("segmentCount", equalTo(0));
    }

    @Test
    void pinSurvivesAndLifts() {
        writeIntent("p-pin", "doc", "fp", "h", 1, "api");
        commit("p-pin", "doc", "fp", "h", 1);

        RestAssured.post("/ingestion-tracker/p-pin/doc/pin").then().body("pinned", equalTo(true));
        RestAssured.post("/ingestion-tracker/p-pin/doc/unpin").then().body("pinned", equalTo(false));
    }

    @Test
    void markFailedRecordsTheAttemptAndKeepsCommittedState() {
        writeIntent("p-fail", "doc", "fp1", "h1", 2, "source");
        commit("p-fail", "doc", "fp1", "h1", 2);

        RestAssured.given().queryParam("fingerprint", "fp2")
                .post("/ingestion-tracker/p-fail/doc/fail")
                .then().statusCode(200)
                .body("status", equalTo("failed"))
                // the failed attempt's fingerprint gates retries
                .body("fingerprint", equalTo("fp2"))
                // previously committed segments keep serving
                .body("segmentCount", equalTo(2));
    }

    private static void writeIntent(String pipeline, String docId, String fingerprint, String contentHash,
            int intendedCount, String origin) {
        RestAssured.given()
                .queryParam("fingerprint", fingerprint).queryParam("contentHash", contentHash)
                .queryParam("intendedCount", intendedCount)
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
