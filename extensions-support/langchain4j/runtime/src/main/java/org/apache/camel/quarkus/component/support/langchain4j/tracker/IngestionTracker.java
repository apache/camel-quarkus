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
package org.apache.camel.quarkus.component.support.langchain4j.tracker;

import java.util.List;
import java.util.Optional;

/**
 * The ingestion tracker: one row per document, the authority on what was ingested. The vector store is
 * a projection that is never asked questions — losing the tracker costs re-ingestion (which
 * converges thanks to deterministic segment ids), never correctness. Reconciliation is
 * tracker-versus-source; the store is never enumerated.
 *
 * <p>
 * <strong>Status: Experimental.</strong> Internal SPI — not a public API. No compatibility
 * guarantees between releases; applications must not implement or call this interface, and it
 * may change or be removed without a deprecation cycle. It exists so the implementation can be
 * replaced wholesale: LangChain4j has no equivalent of LangChain-Python's {@code RecordManager} yet
 * (<a href="https://github.com/langchain4j/langchain4j/issues/2931">langchain4j#2931</a>), and
 * once one lands upstream, an adapter implementing this interface replaces the
 * {@code tracker.jdbc} package while every consumer stays untouched. New implementations must
 * pass the behavioural tests of {@code JdbcIngestionTrackerTest}, whose test methods are written
 * against this SPI only and are meant to be extracted into a shared contract base class the day
 * a second implementation exists.
 *
 * <p>
 * Two method groups, by replaceability:
 * <ul>
 * <li><em>Tracker subset</em> — {@link #ensureSchema}, {@link #read}, {@link #listDocuments},
 * {@link #refreshFingerprint}, {@link #deleteRow}: mirrors what an upstream record manager
 * provides and would delegate to it directly.</li>
 * <li><em>Camel Quarkus extensions</em> — the two-phase {@link #writeIntent}/{@link #commit}
 * protocol, {@link #tombstone}/{@link #unsuppress}, {@link #pin}/{@link #unpin},
 * {@link #markFailed}: product semantics an upstream tracker will not carry; an adapter keeps
 * these in side storage keyed the same way.</li>
 * </ul>
 */
public interface IngestionTracker {

    String ORIGIN_SOURCE = "source";
    String ORIGIN_API = "api";

    /** Creates or migrates the backing schema. Called once before first use. */
    void ensureSchema();

    Optional<TrackerRow> read(String pipeline, String documentId);

    /** All rows of a pipeline — the reconciliation input. */
    List<TrackerRow> listDocuments(String pipeline);

    /**
     * Durably records the intent to (re)write a document <em>before</em> the store is touched.
     * A row left {@code in_progress} by a crash is never skipped by change detection, so the
     * next delivery of the document converges the store.
     */
    void writeIntent(String pipeline, String documentId, String fingerprint, String contentHash,
            int committedCount, int intendedCount, String origin);

    /** Marks the write complete. Only rows in {@code done} status participate in skip decisions. */
    void commit(String pipeline, String documentId, String fingerprint, String contentHash, int segmentCount);

    /**
     * Refreshes the stored fingerprint of an already-{@code done} row whose content turned out
     * unchanged (tier-2 hash match after a tier-1 fingerprint miss), so the cheaper tier-1 check
     * skips the document next time.
     */
    void refreshFingerprint(String pipeline, String documentId, String fingerprint);

    /**
     * Marks an explicit delete: the row survives as a suppression record, so the document stays
     * deleted even while its source file still exists. Lifted with {@link #unsuppress}.
     */
    void tombstone(String pipeline, String documentId);

    void unsuppress(String pipeline, String documentId);

    /** Pins a document: source updates are suppressed until {@link #unpin}. */
    void pin(String pipeline, String documentId);

    void unpin(String pipeline, String documentId);

    /** Removes a row entirely — used by deletion-by-disappearance, where nothing needs remembering. */
    void deleteRow(String pipeline, String documentId);

    /**
     * Dead-letters a document after a processing failure: records the fingerprint of the failed
     * attempt so subsequent passes skip the poison document until its content changes, instead
     * of failing identically forever at cost. Previous committed segments (a stale version that
     * correctly keeps serving) stay untouched.
     */
    void markFailed(String pipeline, String documentId, String fingerprint);

    /**
     * @param status {@code done}, {@code in_progress} or {@code failed}
     * @param origin {@link #ORIGIN_SOURCE} or {@link #ORIGIN_API}
     */
    record TrackerRow(String pipeline, String documentId, String fingerprint, String contentHash,
            int segmentCount, int intendedCount, String status, String origin, boolean tombstone,
            boolean pinned) {

        public boolean done() {
            return "done".equals(status);
        }

        public boolean failed() {
            return "failed".equals(status);
        }

        /** The shrink bound: the largest segment index that may exist in the store, ever intended. */
        public int maxKnownCount() {
            return Math.max(segmentCount, intendedCount);
        }
    }
}
