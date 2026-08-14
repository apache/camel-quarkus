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
package org.apache.camel.quarkus.component.support.langchain4j.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.camel.quarkus.component.support.langchain4j.tracker.IngestionTracker;
import org.jboss.logging.Logger;

/**
 * One bounded synchronisation pass: process every enumerated document, then reconcile —
 * documents the tracker knows but the source no longer lists are deleted. Reconciliation is
 * tracker-versus-source; the store is never enumerated.
 *
 * <p>
 * The safety interlock, because the failure mode of getting deletion wrong is an emptied
 * knowledge base with a green log:
 * <ul>
 * <li><b>Enumeration completeness</b> — the caller only invokes this with a fully built listing;
 * a failed enumeration must abort the pass before this class is reached.</li>
 * <li><b>Zero-failure gate</b> — if any document failed to process, the pass is
 * {@code partially-failed} and deletes nothing: an undelivered document is indistinguishable
 * from a disappeared one.</li>
 * <li><b>Bulk-delete floor</b> — deleting more than the configured fraction of the pipeline's
 * pre-pass corpus in one pass requires explicit consent; a mistyped directory or a failed
 * mount must be a refusal, not an incident. A threshold of {@code 0} means every deletion
 * needs consent.</li>
 * <li>Only {@code origin=source} rows are deletion candidates — API-written documents are never
 * deleted for not appearing in a listing. Tombstoned rows are suppression records, not
 * candidates.</li>
 * </ul>
 *
 * <p>
 * Failures during processing are handled by type: a {@link IngestService.RetryableIngestException}
 * (infrastructure, after the durable intent) leaves the row {@code in_progress} so the next
 * pass retries it — it is <em>not</em> dead-lettered, or a transient provider outage would
 * permanently freeze or drop documents. Content-attributable failures are dead-lettered via
 * {@link IngestionTracker#markFailed} and retried only when the content changes.
 */
public class SyncPassRunner {

    private static final Logger LOG = Logger.getLogger(SyncPassRunner.class);

    /** A source document as enumerated: cheap fingerprint now, content only on demand. */
    public record SourceDocument(String fingerprint, Supplier<String> text, String tenant) {

        public SourceDocument(String fingerprint, Supplier<String> text) {
            this(fingerprint, text, null);
        }
    }

    /**
     * {@code status} is {@code succeeded}, {@code deletions-refused} (processing succeeded but
     * the bulk-delete floor withheld deletions pending consent) or {@code partially-failed}.
     */
    public record PassOutcome(int processed, int failed, int ingested, int replaced, int skippedUnchanged,
            int suppressed, int deadLettered, int staleRetained, int segmentsWritten, int deleted,
            int deletionRefused, String status) {

        /** Every listed document processed without failure — includes {@code deletions-refused}. */
        public boolean succeeded() {
            return !"partially-failed".equals(status);
        }
    }

    private final IngestService service;
    private final IngestionTracker tracker;
    private final String pipeline;
    private final double bulkDeleteThreshold;
    private final boolean allowBulkDelete;
    private final String bulkDeleteConsentHint;

    /**
     * @param bulkDeleteConsentHint appended to the refusal log line: tells the operator how to
     *                              consent (the engine is deliberately ignorant of the consumer's
     *                              configuration namespace)
     */
    public SyncPassRunner(IngestService service, IngestionTracker tracker, String pipeline,
            double bulkDeleteThreshold, boolean allowBulkDelete, String bulkDeleteConsentHint) {
        if (Double.isNaN(bulkDeleteThreshold) || bulkDeleteThreshold < 0 || bulkDeleteThreshold > 1) {
            // NaN would silently disable the floor: Math.max(1, NaN) is NaN and n > NaN is false
            throw new IllegalArgumentException(
                    "bulkDeleteThreshold must be between 0 and 1, got " + bulkDeleteThreshold);
        }
        this.service = service;
        this.tracker = tracker;
        this.pipeline = pipeline;
        this.bulkDeleteThreshold = bulkDeleteThreshold;
        this.allowBulkDelete = allowBulkDelete;
        this.bulkDeleteConsentHint = bulkDeleteConsentHint;
    }

    /**
     * @param listing the complete enumeration of the source: documentId → document. The caller
     *                guarantees completeness — on any enumeration error it must not call this.
     */
    public PassOutcome run(Map<String, SourceDocument> listing) {
        // the floor's denominator is the corpus as it was BEFORE this pass: measuring against
        // post-pass rows would let a large influx of new documents dilute a mass disappearance
        // below the threshold
        int prePassSourceOwned = 0;
        for (IngestionTracker.TrackerRow row : tracker.listDocuments(pipeline)) {
            if (IngestionTracker.ORIGIN_SOURCE.equals(row.origin()) && !row.tombstone()) {
                prePassSourceOwned++;
            }
        }

        int processed = 0;
        int failed = 0;
        int ingested = 0;
        int replaced = 0;
        int skippedUnchanged = 0;
        int suppressed = 0;
        int deadLettered = 0;
        int staleRetained = 0;
        int segmentsWritten = 0;

        for (Map.Entry<String, SourceDocument> entry : listing.entrySet()) {
            String documentId = entry.getKey();
            SourceDocument document = entry.getValue();
            try {
                IngestResult result = service.ingest(documentId, document.fingerprint(),
                        document.text(), IngestService.Origin.SOURCE, document.tenant());
                processed++;
                segmentsWritten += result.segmentsWritten();
                switch (result.outcome()) {
                case INGESTED -> ingested++;
                case REPLACED -> replaced++;
                case SKIPPED_UNCHANGED -> skippedUnchanged++;
                case SUPPRESSED_TOMBSTONE, SUPPRESSED_PINNED -> suppressed++;
                case DEAD_LETTERED -> deadLettered++;
                default -> {
                    // empty: nothing to count
                }
                }
            } catch (IngestService.RetryableIngestException e) {
                // infrastructure failure after the durable intent: the in_progress row makes the
                // next pass retry and converge — dead-lettering here would freeze (or, after a
                // remove-then-add, permanently drop) the document on a transient outage
                failed++;
                if (e.committedVersionServes()) {
                    staleRetained++;
                    LOG.errorf(e, "Pipeline '%s': failed to write '%s' after the intent — retried next "
                            + "pass; the STALE previous version keeps serving; deletion disabled for this "
                            + "pass", pipeline, documentId);
                } else if (e.oldGenerationRemoved()) {
                    LOG.errorf(e, "Pipeline '%s': failed to write '%s' after its previous generation was "
                            + "removed — the document is UNAVAILABLE until a retry succeeds (next pass); "
                            + "deletion disabled for this pass", pipeline, documentId);
                } else {
                    LOG.errorf(e, "Pipeline '%s': failed to write '%s' after the intent — retried next "
                            + "pass; deletion disabled for this pass", pipeline, documentId);
                }
            } catch (Exception e) {
                // content-attributable (or unexpected): dead-letter — retried when content changes
                failed++;
                // the stored fingerprint carries the tenant (see IngestService.effectiveFingerprint),
                // so the recorded gate must too — a raw fingerprint would never match again and the
                // poison document would be re-fetched and re-embedded on every pass
                String dedupFingerprint = IngestService.effectiveFingerprint(document.fingerprint(),
                        document.tenant());
                boolean hadCommittedVersion;
                if (e instanceof IngestService.ContentIngestException content) {
                    dedupFingerprint = content.dedupFingerprint();
                    hadCommittedVersion = content.committedVersionServes();
                } else {
                    hadCommittedVersion = tracker.read(pipeline, documentId)
                            .map(row -> row.done() && row.segmentCount() > 0)
                            .orElse(false);
                }
                if (hadCommittedVersion) {
                    staleRetained++;
                }
                tracker.markFailed(pipeline, documentId, dedupFingerprint);
                LOG.errorf(e, "Pipeline '%s': failed to process '%s' — dead-lettered (retried when its "
                        + "content changes)%s; deletion disabled for this pass", pipeline, documentId,
                        hadCommittedVersion ? "; the STALE previous version keeps serving" : "");
            }
        }

        if (failed > 0) {
            return new PassOutcome(processed, failed, ingested, replaced, skippedUnchanged, suppressed,
                    deadLettered, staleRetained, segmentsWritten, 0, 0, "partially-failed");
        }

        // reconcile: tracker-versus-source
        List<IngestionTracker.TrackerRow> rows = tracker.listDocuments(pipeline);
        List<IngestionTracker.TrackerRow> candidates = new ArrayList<>();
        for (IngestionTracker.TrackerRow row : rows) {
            if (!IngestionTracker.ORIGIN_SOURCE.equals(row.origin()) || row.tombstone()) {
                continue;
            }
            // any status is a candidate, including in_progress: a crashed mid-ingest row whose
            // document then disappeared from the source would otherwise never be reclaimed —
            // not in the listing (never re-processed), previously not a candidate either, its
            // row and partially written vectors leaking forever. The interlock above already
            // guarantees a complete, failure-free pass, and concurrent API writers are excluded
            // by origin (an API writeIntent stamps origin=api).
            if (!listing.containsKey(row.documentId())) {
                candidates.add(row);
            }
        }

        // threshold 0 = every deletion needs consent; otherwise deleting ONE document is never
        // "bulk" — a single-document pipeline (an http url) could otherwise never delete at all,
        // since 1 of 1 is always over any fractional threshold
        double allowed = bulkDeleteThreshold == 0 ? 0 : Math.max(1, bulkDeleteThreshold * prePassSourceOwned);
        if (!candidates.isEmpty() && !allowBulkDelete && candidates.size() > allowed) {
            LOG.warnf("Pipeline '%s': refusing to delete %d of %d source documents in one pass "
                    + "(threshold %.0f%%). If this is intended (corpus restructuring), %s.",
                    pipeline, candidates.size(), prePassSourceOwned, bulkDeleteThreshold * 100,
                    bulkDeleteConsentHint);
            return new PassOutcome(processed, 0, ingested, replaced, skippedUnchanged, suppressed,
                    deadLettered, staleRetained, segmentsWritten, 0, candidates.size(), "deletions-refused");
        }

        int deleted = 0;
        for (IngestionTracker.TrackerRow row : candidates) {
            List<String> ids = new ArrayList<>(row.maxKnownCount());
            for (int i = 0; i < row.maxKnownCount(); i++) {
                ids.add(IngestIds.segmentId(pipeline, row.documentId(), i));
            }
            service.removeSegments(ids);
            tracker.deleteRow(pipeline, row.documentId());
            deleted++;
            LOG.infof("Pipeline '%s': document '%s' disappeared from the source — removed", pipeline,
                    row.documentId());
        }

        return new PassOutcome(processed, 0, ingested, replaced, skippedUnchanged, suppressed,
                deadLettered, staleRetained, segmentsWritten, deleted, 0, "succeeded");
    }
}
