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
package org.apache.camel.quarkus.component.support.langchain4j.tracker.jdbc;

import java.util.List;
import java.util.UUID;

import org.apache.camel.quarkus.component.support.langchain4j.tracker.IngestionTracker;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The behavioural contract of the {@link IngestionTracker} SPI, exercised against the JDBC
 * implementation on H2. The test methods are deliberately written against the SPI only — nothing
 * below {@link #setUpTracker()} references {@link JdbcIngestionTracker}.
 *
 * <p>
 * The contract lives folded into this class only because a single implementation exists today.
 * When a second one arrives — e.g. an adapter over a future upstream LangChain4j record manager
 * (langchain4j#2931) — extract the test methods into an abstract {@code IngestionTrackerContract}
 * base class with a {@code createTracker()} factory, and keep one {@code *Test} subclass per
 * implementation: a replacement is a drop-in exactly when its subclass passes.
 */
class JdbcIngestionTrackerTest {

    IngestionTracker tracker;

    /** A fresh, empty tracker per test. */
    @BeforeEach
    void setUpTracker() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        tracker = new JdbcIngestionTracker(dataSource);
        tracker.ensureSchema();
    }

    @Test
    void unknownDocumentReadsEmpty() {
        assertTrue(tracker.read("p", "missing").isEmpty());
    }

    @Test
    void intentIsDurableAndNeverSkippable() {
        tracker.writeIntent("p", "doc", "fp1", "hash1", 5, IngestionTracker.ORIGIN_SOURCE);

        IngestionTracker.TrackerRow row = tracker.read("p", "doc").orElseThrow();
        assertFalse(row.done(), "an intent row must not count as done");
        assertEquals(5, row.maxKnownCount(), "the shrink bound must cover the intended count");
    }

    @Test
    void commitCompletesTheIntent() {
        tracker.writeIntent("p", "doc", "fp1", "hash1", 5, IngestionTracker.ORIGIN_SOURCE);
        tracker.commit("p", "doc", "fp1", "hash1", 3);

        IngestionTracker.TrackerRow row = tracker.read("p", "doc").orElseThrow();
        assertTrue(row.done());
        assertEquals("fp1", row.fingerprint());
        assertEquals("hash1", row.contentHash());
        assertEquals(3, row.segmentCount());
    }

    @Test
    void commitWithoutIntentThrows() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> tracker.commit("p", "ghost", "fp", "h", 1));
        assertTrue(e.getMessage().contains("preceding intent"), e.getMessage());
    }

    @Test
    void reintentKeepsTheLargestKnownCount() {
        tracker.writeIntent("p", "doc", "fp1", "hash1", 5, IngestionTracker.ORIGIN_SOURCE);
        tracker.commit("p", "doc", "fp1", "hash1", 5);
        // a re-intent does not change what the store really holds — the five old segments are
        // still in it, so the committed count has to stay; only commit, which runs after the
        // stale tail was actually removed, may lower the bound
        tracker.writeIntent("p", "doc", "fp2", "hash2", 2, IngestionTracker.ORIGIN_SOURCE);

        assertEquals(5, tracker.read("p", "doc").orElseThrow().maxKnownCount(),
                "shrink must still see the previously committed tail");
    }

    @Test
    void reintentBeforeCommitKeepsTheLargestIntendedCount() {
        tracker.writeIntent("p", "doc", "fp1", "hash1", 5, IngestionTracker.ORIGIN_SOURCE);
        // the consumer may have written up to 5 segments before dying without a commit; a
        // smaller re-intent must not lower the sweep bound below what may exist in the store
        tracker.writeIntent("p", "doc", "fp2", "hash2", 2, IngestionTracker.ORIGIN_SOURCE);

        assertEquals(5, tracker.read("p", "doc").orElseThrow().maxKnownCount(),
                "an uncommitted intent must never lower the sweep bound");
    }

    @Test
    void refreshFingerprintTouchesNothingElse() {
        tracker.writeIntent("p", "doc", "fp1", "hash1", 2, IngestionTracker.ORIGIN_SOURCE);
        tracker.commit("p", "doc", "fp1", "hash1", 2);
        tracker.refreshFingerprint("p", "doc", "fp2");

        IngestionTracker.TrackerRow row = tracker.read("p", "doc").orElseThrow();
        assertEquals("fp2", row.fingerprint());
        assertEquals("hash1", row.contentHash());
        assertEquals(2, row.segmentCount());
        assertTrue(row.done());
    }

    @Test
    void refreshFingerprintIgnoresRowsThatAreNotDone() {
        tracker.writeIntent("p", "doc", "fp1", "h1", 1, IngestionTracker.ORIGIN_SOURCE);
        tracker.markFailed("p", "doc", "fp-failed");
        tracker.refreshFingerprint("p", "doc", "fp-new");

        assertEquals("fp-failed", tracker.read("p", "doc").orElseThrow().fingerprint(),
                "a refresh must not re-arm the dead-letter gate of a failed row");
    }

    @Test
    void listDocumentsIsIsolatedByPipeline() {
        tracker.writeIntent("p1", "a", "fp", "h", 1, IngestionTracker.ORIGIN_SOURCE);
        tracker.commit("p1", "a", "fp", "h", 1);
        tracker.writeIntent("p2", "b", "fp", "h", 1, IngestionTracker.ORIGIN_SOURCE);
        tracker.commit("p2", "b", "fp", "h", 1);

        List<IngestionTracker.TrackerRow> rows = tracker.listDocuments("p1");
        assertEquals(1, rows.size());
        assertEquals("a", rows.get(0).documentId());
    }

    @Test
    void deleteRowForgetsTheDocument() {
        tracker.writeIntent("p", "doc", "fp", "h", 1, IngestionTracker.ORIGIN_SOURCE);
        tracker.commit("p", "doc", "fp", "h", 1);
        tracker.deleteRow("p", "doc");

        assertTrue(tracker.read("p", "doc").isEmpty());
        assertTrue(tracker.listDocuments("p").isEmpty());
    }

    @Test
    void tombstoneSurvivesAndLifts() {
        tracker.writeIntent("p", "doc", "fp", "h", 1, IngestionTracker.ORIGIN_SOURCE);
        tracker.commit("p", "doc", "fp", "h", 1);

        tracker.tombstone("p", "doc");
        assertTrue(tracker.read("p", "doc").orElseThrow().tombstone(),
                "a tombstoned row must survive as a suppression record");

        tracker.unsuppress("p", "doc");
        assertFalse(tracker.read("p", "doc").orElseThrow().tombstone());
    }

    @Test
    void tombstoneOfANeverIngestedDocumentCreatesTheSuppressionRow() {
        tracker.tombstone("p", "ghost");

        IngestionTracker.TrackerRow row = tracker.read("p", "ghost").orElseThrow();
        assertTrue(row.tombstone(), "a pure suppression row must be created, or the suppression "
                + "would not survive for a document deleted before its first ingest");
        assertTrue(row.done());
        assertEquals(0, row.segmentCount());
    }

    @Test
    void pinSurvivesAndLifts() {
        tracker.writeIntent("p", "doc", "fp", "h", 1, IngestionTracker.ORIGIN_API);
        tracker.commit("p", "doc", "fp", "h", 1);

        tracker.pin("p", "doc");
        assertTrue(tracker.read("p", "doc").orElseThrow().pinned());

        tracker.unpin("p", "doc");
        assertFalse(tracker.read("p", "doc").orElseThrow().pinned());
    }

    @Test
    void markFailedRecordsTheAttemptAndKeepsCommittedState() {
        tracker.writeIntent("p", "doc", "fp1", "h1", 2, IngestionTracker.ORIGIN_SOURCE);
        tracker.commit("p", "doc", "fp1", "h1", 2);
        // the failed attempt's fingerprint has to be persisted, so the tracker knows it was fp2
        // that failed: passes skip the poison document while it still fingerprints as fp2 and
        // retry only once the content changes (keeping fp1 would retry — and fail — every pass)
        tracker.markFailed("p", "doc", "fp2");

        IngestionTracker.TrackerRow row = tracker.read("p", "doc").orElseThrow();
        assertTrue(row.failed());
        assertEquals("fp2", row.fingerprint(), "the failed attempt's fingerprint gates retries");
        assertEquals(2, row.segmentCount(), "previously committed segments keep serving");
    }

    @Test
    void markFailedFromInProgressKeepsTheBound() {
        tracker.writeIntent("p", "doc", "fp1", "h1", 3, IngestionTracker.ORIGIN_SOURCE);
        tracker.markFailed("p", "doc", "fp1");

        IngestionTracker.TrackerRow row = tracker.read("p", "doc").orElseThrow();
        assertTrue(row.failed());
        assertEquals(0, row.segmentCount(), "nothing was ever committed");
        assertEquals(3, row.maxKnownCount(),
                "the failed intent's bound must survive — up to 3 segments may sit in the store");
    }

    @Test
    void preProvisionedModeRequiresTheTableToExist() {
        JdbcDataSource fresh = new JdbcDataSource();
        fresh.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");

        JdbcIngestionTracker probing = new JdbcIngestionTracker(fresh, false);
        assertThrows(IllegalStateException.class, probing::ensureSchema,
                "without DDL rights the tracker must refuse loudly, not fail on first use");

        new JdbcIngestionTracker(fresh).ensureSchema();
        probing.ensureSchema();
    }
}
