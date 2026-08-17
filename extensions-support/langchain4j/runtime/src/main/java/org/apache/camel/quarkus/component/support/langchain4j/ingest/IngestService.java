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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.exception.UnsupportedFeatureException;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.camel.quarkus.component.support.langchain4j.tracker.IngestionTracker;
import org.jboss.logging.Logger;

/**
 * Ingests documents into an embedding store with deterministic segment ids. Deliberately free of
 * any Camel dependency — this package is the sync-engine core.
 *
 * <p>
 * Two modes:
 * <ul>
 * <li><b>append</b> ({@code tracker == null}) — write-only; re-ingestion of an unchanged document
 * costs a write (harmless on upsert-capable stores thanks to deterministic ids, duplicating on
 * others).</li>
 * <li><b>sync</b> — the full protocol: tier-1 fingerprint check, tier-2 content-hash check,
 * tracker intent, write (per-store strategy), shrink-remove, tracker commit. Crash anywhere
 * converges on the next delivery because the intent row is never skipped and the shrink bound is
 * {@code max(committed, ever-intended)} — the invariants the crash-convergence tests pin.</li>
 * </ul>
 *
 * <p>
 * Failures are typed by where they happen, because the consequences differ:
 * {@link ContentIngestException} (before the durable intent — fetching, splitting: attributable
 * to the content, dead-letter material) versus {@link RetryableIngestException} (after the
 * intent — embedding, store or tracker I/O: infrastructure, the row stays {@code in_progress}
 * and the next delivery retries and converges). Conflating the two would let a transient
 * provider outage permanently freeze — or, on remove-then-add stores, permanently drop — a
 * document while passes report success.
 *
 * <p>
 * <b>Concurrency:</b> writes are serialized per document id inside this instance (striped
 * locks), which upholds the tracker's single-writer-per-{@code (pipeline, documentId)} invariant
 * for the one instance a pipeline owns — the source pass and API/ingress writers share it.
 * Across replicas there is no coordination by design: deterministic ids make concurrent writers
 * converge; the duplicate cost is bounded and the tracker heals on the next pass.
 */
public class IngestService {

    private static final Logger LOG = Logger.getLogger(IngestService.class);

    /** Bumping this re-embeds every corpus; it is part of the content hash on purpose. */
    static final String SCHEMA_VERSION = "1";
    /** This release parses bytes as UTF-8 text; the parser identity is a hash input. */
    static final String PARSER_ID = "text";

    /** Retrieval-facing segment metadata: what citations and tenancy filters are built on. */
    public static final String METADATA_PIPELINE = "cq_pipeline";
    public static final String METADATA_DOCUMENT_ID = "cq_document_id";
    public static final String METADATA_TENANT = "cq_tenant";

    /** Stores enforce per-request id limits; removals are batched like writes are. */
    static final int REMOVE_BATCH_SIZE = 256;

    private static final int LOCK_STRIPES = 64;

    /**
     * Embedding rate limiting is scoped to the model instance, not the pipeline: two pipelines
     * sharing one provider must share the budget, or the provider sees a multiple of the
     * configured rate. Values are "earliest next call" {@code nanoTime} stamps. Weak keys, so
     * models discarded by dev-mode reloads or tests are not pinned for the JVM lifetime;
     * compound operations synchronize on the map itself.
     */
    private static final Map<EmbeddingModel, long[]> MODEL_CALL_GATES = Collections
            .synchronizedMap(new WeakHashMap<>());

    public enum WriteStrategy {
        /** Same-id write overwrites (pgvector, qdrant, elasticsearch — measured behaviour). */
        UPSERT,
        /**
         * Same-id write silently keeps or mixes old content (chroma, milvus, in-memory) — remove
         * the previous generation's ids first.
         */
        REMOVE_THEN_ADD;

        public static WriteStrategy of(String value) {
            return switch (value) {
            case "upsert" -> UPSERT;
            case "remove-then-add" -> REMOVE_THEN_ADD;
            default -> throw new IllegalArgumentException(
                    "Unknown write-strategy '" + value + "'. Supported: upsert, remove-then-add");
            };
        }
    }

    /** A failure attributable to the document's content, raised before the durable intent. */
    public static final class ContentIngestException extends RuntimeException {

        private final String dedupFingerprint;
        private final boolean committedVersionServes;

        ContentIngestException(Throwable cause, String dedupFingerprint, boolean committedVersionServes) {
            super(cause.getMessage(), cause);
            this.dedupFingerprint = dedupFingerprint;
            this.committedVersionServes = committedVersionServes;
        }

        /**
         * What to record with {@link IngestionTracker#markFailed}: the effective fingerprint if
         * the source supplied one, else the content hash if the content was readable, else
         * {@code null} (no dead-letter gate — the failure is retried).
         */
        public String dedupFingerprint() {
            return dedupFingerprint;
        }

        /** A previously committed version exists and keeps serving — stale, but valid. */
        public boolean committedVersionServes() {
            return committedVersionServes;
        }
    }

    /**
     * An infrastructure failure raised after the durable intent (embedding, store or tracker
     * I/O). The tracker row remains {@code in_progress}, which change detection never skips, so
     * the next delivery retries and converges — such failures must never be dead-lettered, or a
     * transient outage would permanently freeze (or drop) the document.
     */
    public static final class RetryableIngestException extends RuntimeException {

        private final boolean committedVersionServes;
        private final boolean oldGenerationRemoved;

        RetryableIngestException(Throwable cause, boolean committedVersionServes, boolean oldGenerationRemoved) {
            super(cause.getMessage(), cause);
            this.committedVersionServes = committedVersionServes;
            this.oldGenerationRemoved = oldGenerationRemoved;
        }

        /** A previously committed version is still in the store and keeps serving. */
        public boolean committedVersionServes() {
            return committedVersionServes;
        }

        /**
         * The remove-then-add strategy had already removed the old generation when the write
         * failed: the document is offline until a retry succeeds.
         */
        public boolean oldGenerationRemoved() {
            return oldGenerationRemoved;
        }
    }

    private final String pipeline;
    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel model;
    private final DocumentSplitter splitter;
    private final IngestionTracker tracker;
    private final WriteStrategy writeStrategy;
    private final String hashSuffix;
    private final Object[] documentLocks;
    private volatile int embeddingBatchSize = 32;
    private volatile long minMillisBetweenEmbeddingCalls;

    public IngestService(String pipeline, EmbeddingStore<TextSegment> store, EmbeddingModel model,
            String splitterKind, int maxSegmentSize, int maxOverlapSize,
            IngestionTracker tracker, WriteStrategy writeStrategy, String embeddingModelId) {
        if (pipeline == null || pipeline.isBlank()) {
            throw new IllegalArgumentException("An ingestion pipeline needs a non-blank name");
        }
        this.pipeline = pipeline;
        this.store = Objects.requireNonNull(store, "store");
        this.model = Objects.requireNonNull(model, "model");
        this.tracker = tracker;
        this.writeStrategy = Objects.requireNonNull(writeStrategy, "writeStrategy");
        this.splitter = switch (splitterKind) {
        case "recursive" -> DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize);
        case "none" -> null;
        default -> throw new IllegalArgumentException(
                "Unknown splitter '" + splitterKind + "' for ingestion pipeline '" + pipeline
                        + "'. Supported: recursive, none");
        };
        // every non-content input that must invalidate change detection when it changes:
        // model identity, splitter parameters, parser identity, schema version
        this.hashSuffix = "\0" + embeddingModelId + "\0" + splitterKind + "\0" + maxSegmentSize
                + "\0" + maxOverlapSize + "\0" + PARSER_ID + "\0" + SCHEMA_VERSION;
        this.documentLocks = new Object[LOCK_STRIPES];
        for (int i = 0; i < LOCK_STRIPES; i++) {
            documentLocks[i] = new Object();
        }
        if (tracker != null) {
            probeRemovalCapability();
        }
    }

    /**
     * Sync mode is built on id-addressed removal, but {@code EmbeddingStore.removeAll(ids)} is a
     * default-throwing SPI method — a store may simply not have it. Probing once at construction
     * (removing an id that cannot exist is a no-op on capable stores) turns "every document of
     * every pass dead-letters, then the pass reports success" into a fail-fast with the store's
     * name in it.
     */
    private void probeRemovalCapability() {
        try {
            // a random id cannot collide with any document's segments; removing a nonexistent id
            // is a no-op on capable stores
            store.removeAll(List.of(UUID.randomUUID().toString()));
        } catch (UnsupportedFeatureException | UnsupportedOperationException e) {
            throw new IllegalStateException(
                    "The embedding store " + store.getClass().getName() + " does not support id-addressed "
                            + "removal (removeAll(ids)), which sync mode's replacement and deletion are built "
                            + "on. Use mode=append or a removal-capable store.",
                    e);
        } catch (RuntimeException e) {
            // a store that is merely unreachable at startup is not an incapable store: let the
            // first pass fail and retry rather than turning a blip into a hard startup failure
            LOG.warnf(e, "Pipeline '%s': embedding store capability probe inconclusive (store not "
                    + "reachable) — proceeding; the first synchronisation pass will retry", pipeline);
        }
    }

    /** Who is writing: the pipeline's own source scan, or application code / the ingress. */
    public enum Origin {
        SOURCE(IngestionTracker.ORIGIN_SOURCE),
        API(IngestionTracker.ORIGIN_API);

        final String value;

        Origin(String value) {
            this.value = value;
        }
    }

    /** No-fingerprint convenience (any mode): change detection falls back to the content hash. */
    public IngestResult ingest(String documentId, String text) {
        return ingest(documentId, null, text, Origin.SOURCE);
    }

    /** Source-origin convenience. */
    public IngestResult ingest(String documentId, String fingerprint, String text) {
        return ingest(documentId, fingerprint, text, Origin.SOURCE);
    }

    public IngestResult ingest(String documentId, String fingerprint, String text, Origin origin) {
        return ingest(documentId, fingerprint, () -> text, origin, null);
    }

    public IngestResult ingest(String documentId, String fingerprint, String text, Origin origin, String tenant) {
        return ingest(documentId, fingerprint, () -> text, origin, tenant);
    }

    public IngestResult ingest(String documentId, String fingerprint, Supplier<String> text, Origin origin) {
        return ingest(documentId, fingerprint, text, origin, null);
    }

    /**
     * The supplier form: content is only fetched when change detection cannot skip the document
     * — an unchanged corpus costs neither read nor parse. The optional tenant is written as
     * segment metadata for retrieval-side isolation and participates in change detection: a
     * document re-assigned to another tenant is re-written, never skipped with stale metadata.
     */
    public IngestResult ingest(String documentId, String fingerprint, Supplier<String> text, Origin origin,
            String tenant) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException(
                    "A stable document id is required to ingest into pipeline '" + pipeline + "'");
        }
        if (tracker == null) {
            String content = text.get();
            if (content == null || content.isBlank()) {
                return new IngestResult(pipeline, documentId, 0, IngestResult.Outcome.EMPTY);
            }
            return append(documentId, content, tenant);
        }
        // the source pass and API/ingress writers share this instance: serialize per document
        synchronized (lockFor(documentId)) {
            return sync(documentId, fingerprint, text, origin, tenant);
        }
    }

    /**
     * Serializes the whole read→intent→write→commit window per document id — the in-JVM half of
     * the tracker's single-writer invariant. 64 fixed stripes bound memory (no per-id lock map);
     * the same id always hashes to the same stripe, distinct ids rarely share one.
     */
    private Object lockFor(String documentId) {
        return documentLocks[Math.floorMod(documentId.hashCode(), LOCK_STRIPES)];
    }

    /**
     * Explicit delete: a tombstone recorded first, then vectors removed, so the document
     * <em>stays</em> deleted even while its source file still exists — a legal hold that reverts
     * on the next poll would be worse than none. The tombstone is written before the store is
     * touched on purpose: a crash mid-delete then fails toward "suppressed, with lingering
     * vectors" — recoverable by retrying the delete (or by unsuppress + re-ingest, which
     * overwrites the leftovers) — never toward a delete that silently reverts. Lifted with
     * {@link #unsuppress}.
     */
    public IngestResult delete(String documentId) {
        requireSync("delete");
        synchronized (lockFor(documentId)) {
            Optional<IngestionTracker.TrackerRow> existing = tracker.read(pipeline, documentId);
            // for a never-ingested document the tracker itself creates the pure suppression row
            tracker.tombstone(pipeline, documentId);
            int maxKnownCount = existing.map(IngestionTracker.TrackerRow::maxKnownCount).orElse(0);
            if (maxKnownCount > 0) {
                removeSegments(ids(documentId, maxKnownCount));
            }
            tracker.commit(pipeline, documentId, null, null, 0);
            return new IngestResult(pipeline, documentId, 0, IngestResult.Outcome.DELETED);
        }
    }

    /** Lifts a tombstone; the next source pass re-ingests the document if it still exists. */
    public void unsuppress(String documentId) {
        requireSync("unsuppress");
        tracker.unsuppress(pipeline, documentId);
    }

    public void pin(String documentId) {
        requireSync("pin");
        tracker.pin(pipeline, documentId);
    }

    public void unpin(String documentId) {
        requireSync("unpin");
        tracker.unpin(pipeline, documentId);
    }

    private void requireSync(String operation) {
        if (tracker == null) {
            throw new IllegalStateException(
                    "Operation '" + operation + "' needs mode=sync on pipeline '" + pipeline
                            + "' (append mode has no tracker to record it in)");
        }
    }

    /** Store-side removal by explicit ids — batched, stores enforce per-request id limits. */
    void removeSegments(List<String> ids) {
        for (int from = 0; from < ids.size(); from += REMOVE_BATCH_SIZE) {
            store.removeAll(ids.subList(from, Math.min(from + REMOVE_BATCH_SIZE, ids.size())));
        }
    }

    private IngestResult append(String documentId, String text, String tenant) {
        List<TextSegment> segments = withMetadata(split(text), documentId, tenant);
        write(segments, ids(documentId, segments.size()));
        return new IngestResult(pipeline, documentId, segments.size(), IngestResult.Outcome.INGESTED);
    }

    private IngestResult sync(String documentId, String fingerprint, Supplier<String> textSupplier, Origin origin,
            String tenant) {
        Optional<IngestionTracker.TrackerRow> existing;
        try {
            existing = tracker.read(pipeline, documentId);
        } catch (RuntimeException e) {
            throw new RetryableIngestException(e, false, false);
        }
        boolean committedServes = existing.map(row -> row.done() && row.segmentCount() > 0).orElse(false);

        // suppressions come first: an explicitly deleted document stays deleted (any writer),
        // and a pinned document ignores its source until unpinned
        if (existing.isPresent() && existing.get().tombstone()) {
            return new IngestResult(pipeline, documentId, 0, IngestResult.Outcome.SUPPRESSED_TOMBSTONE);
        }
        if (existing.isPresent() && existing.get().pinned() && origin == Origin.SOURCE) {
            return new IngestResult(pipeline, documentId, 0, IngestResult.Outcome.SUPPRESSED_PINNED);
        }

        // the tenant participates in identity: a re-assigned document must not be skipped, so
        // the effective fingerprint (stored and compared) carries the tenant
        String effectiveFingerprint = effectiveFingerprint(fingerprint, tenant);

        // dead-letter: a previous attempt failed on exactly this content — skip until it changes,
        // so a poison document does not fail identically on every pass, forever, at cost
        if (existing.isPresent() && existing.get().failed() && effectiveFingerprint != null
                && effectiveFingerprint.equals(existing.get().fingerprint())) {
            return new IngestResult(pipeline, documentId, 0, IngestResult.Outcome.DEAD_LETTERED);
        }

        // an API write over a source-owned document is a correction: pin it, so the next source
        // pass does not silently revert it (lifted with unpin)
        boolean pinAfterWrite = origin == Origin.API && existing.isPresent()
                && IngestionTracker.ORIGIN_SOURCE.equals(existing.get().origin());

        // tier 1: cheap fingerprint — no read, no hash, no split, no embedding
        if (effectiveFingerprint != null && existing.isPresent() && existing.get().done()
                && effectiveFingerprint.equals(existing.get().fingerprint())) {
            return new IngestResult(pipeline, documentId, 0, IngestResult.Outcome.SKIPPED_UNCHANGED);
        }

        String text;
        try {
            text = textSupplier.get();
        } catch (RuntimeException e) {
            // no content to attribute the failure to: dead-letter on the fingerprint if the
            // source supplied one, otherwise retry on every pass
            throw new ContentIngestException(e, effectiveFingerprint, committedServes);
        }
        if (text == null || text.isBlank()) {
            // deliberately "keep the previous version": blank content is indistinguishable from a
            // transient source glitch (truncated read, file mid-write), and emptying a knowledge
            // base on a glitch would be worse than serving a stale document. An intentional
            // removal is a delete (explicit, or the document disappearing from the source).
            return new IngestResult(pipeline, documentId, 0, IngestResult.Outcome.EMPTY);
        }

        // tier 2: content hash (covers model/splitter/parser/schema/tenant changes exactly)
        String contentHash = contentHash(text, tenant);
        if (existing.isPresent() && existing.get().done() && contentHash.equals(existing.get().contentHash())) {
            if (effectiveFingerprint != null) {
                try {
                    tracker.refreshFingerprint(pipeline, documentId, effectiveFingerprint);
                } catch (RuntimeException e) {
                    // tracker blip on an unchanged, correct document: retryable, never a reason
                    // to dead-letter it and disable the pass's deletions
                    throw new RetryableIngestException(e, committedServes, false);
                }
            }
            return new IngestResult(pipeline, documentId, 0, IngestResult.Outcome.SKIPPED_UNCHANGED);
        }

        // dead-letter fallback for fingerprint-less sources: the failed attempt recorded the
        // content hash in the fingerprint column, so the poison document is recognized after
        // one cheap fetch + hash instead of re-splitting and re-embedding on every pass
        if (existing.isPresent() && existing.get().failed()
                && contentHash.equals(existing.get().fingerprint())) {
            return new IngestResult(pipeline, documentId, 0, IngestResult.Outcome.DEAD_LETTERED);
        }

        List<TextSegment> segments;
        try {
            segments = withMetadata(split(text), documentId, tenant);
        } catch (RuntimeException e) {
            String dedup = effectiveFingerprint != null ? effectiveFingerprint : contentHash;
            throw new ContentIngestException(e, dedup, committedServes);
        }
        int newCount = segments.size();
        int maxKnownCount = existing.map(IngestionTracker.TrackerRow::maxKnownCount).orElse(0);
        boolean replace = maxKnownCount > 0;

        // 1. intent — durable before the store is touched; a crash from here on converges,
        //    because this row is never skipped and the shrink bound below survives in it
        try {
            tracker.writeIntent(pipeline, documentId, effectiveFingerprint, contentHash, newCount, origin.value);
        } catch (RuntimeException e) {
            // tracker I/O is infrastructure: no durable intent means nothing was written and the
            // row is unchanged — the next pass simply retries; dead-lettering here would freeze
            // the changed document on a pool blip or a replica race
            throw new RetryableIngestException(e, committedServes, false);
        }

        // from the intent on, failures are infrastructure: the in_progress row makes the next
        // delivery retry and converge, so they must NOT be dead-lettered
        boolean oldGenerationRemoved = false;
        try {
            // 2. the store write, by measured store capability
            List<String> ids = ids(documentId, newCount);
            if (writeStrategy == WriteStrategy.REMOVE_THEN_ADD && maxKnownCount > 0) {
                removeSegments(ids(documentId, maxKnownCount));
                oldGenerationRemoved = true;
            }
            write(segments, ids);

            // 3. shrink-remove: ids beyond the new count, up to the largest count ever intended
            if (writeStrategy == WriteStrategy.UPSERT && maxKnownCount > newCount) {
                List<String> stale = new ArrayList<>(maxKnownCount - newCount);
                for (int i = newCount; i < maxKnownCount; i++) {
                    stale.add(IngestIds.segmentId(pipeline, documentId, i));
                }
                removeSegments(stale);
            }

            // 4. commit
            tracker.commit(pipeline, documentId, effectiveFingerprint, contentHash, newCount);
        } catch (RuntimeException e) {
            throw new RetryableIngestException(e, committedServes && !oldGenerationRemoved, oldGenerationRemoved);
        }

        if (pinAfterWrite) {
            tracker.pin(pipeline, documentId);
        }

        return new IngestResult(pipeline, documentId, newCount,
                replace ? IngestResult.Outcome.REPLACED : IngestResult.Outcome.INGESTED);
    }

    /** The stored/compared fingerprint carries the tenant, so a tenant change is a change. */
    static String effectiveFingerprint(String fingerprint, String tenant) {
        if (fingerprint == null) {
            return null;
        }
        return tenant == null || tenant.isBlank() ? fingerprint : fingerprint + "\0" + tenant;
    }

    private List<TextSegment> split(String text) {
        return splitter == null
                ? List.of(TextSegment.from(text))
                : splitter.split(Document.from(text));
    }

    /** Retrieval-facing metadata: citations (which document answered) and tenancy filters. */
    private List<TextSegment> withMetadata(List<TextSegment> segments, String documentId, String tenant) {
        for (TextSegment segment : segments) {
            Metadata metadata = segment.metadata()
                    .put(METADATA_PIPELINE, pipeline)
                    .put(METADATA_DOCUMENT_ID, documentId);
            if (tenant != null && !tenant.isBlank()) {
                metadata.put(METADATA_TENANT, tenant);
            }
        }
        return segments;
    }

    private List<String> ids(String documentId, int count) {
        List<String> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(IngestIds.segmentId(pipeline, documentId, i));
        }
        return ids;
    }

    /** Embedding batch size and optional rate limit; provider limits are real. */
    public IngestService embeddingLimits(int batchSize, Integer requestsPerMinute) {
        this.embeddingBatchSize = Math.max(1, batchSize);
        this.minMillisBetweenEmbeddingCalls = requestsPerMinute == null || requestsPerMinute <= 0
                ? 0
                : 60_000L / requestsPerMinute;
        return this;
    }

    private void write(List<TextSegment> segments, List<String> ids) {
        int batchSize = embeddingBatchSize;
        for (int from = 0; from < segments.size(); from += batchSize) {
            int to = Math.min(from + batchSize, segments.size());
            List<TextSegment> batch = segments.subList(from, to);
            throttleEmbedding();
            List<Embedding> embeddings = model.embedAll(batch).content();
            store.addAll(ids.subList(from, to), embeddings, batch);
        }
    }

    /**
     * Claims the next call slot under a short lock, then sleeps outside it. The gate is shared
     * by every pipeline using the same model instance — the provider budget is per provider,
     * not per pipeline. Intervals are measured with {@code nanoTime} (monotonic — an NTP step
     * must not stall embedding). An interrupt aborts the write (restoring the flag): during
     * shutdown a half-issued batch is worse than none, and the in_progress row retries it
     * anyway. Known trade-off: the sleep happens while the caller's document stripe lock is
     * held, so with an aggressive limit unrelated documents sharing the stripe wait too —
     * acceptable for a per-document write path, revisit if limits below ~10 rpm become common.
     */
    private void throttleEmbedding() {
        long min = minMillisBetweenEmbeddingCalls;
        if (min <= 0) {
            return;
        }
        long minNanos = min * 1_000_000L;
        long[] gate;
        synchronized (MODEL_CALL_GATES) {
            gate = MODEL_CALL_GATES.computeIfAbsent(model, m -> new long[] { Long.MIN_VALUE / 2 });
        }
        long wakeAt;
        synchronized (gate) {
            long now = System.nanoTime();
            wakeAt = Math.max(now, gate[0] + minNanos);
            gate[0] = wakeAt;
        }
        long waitNanos = wakeAt - System.nanoTime();
        if (waitNanos > 0) {
            try {
                Thread.sleep(waitNanos / 1_000_000L, (int) (waitNanos % 1_000_000L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while rate-limiting embedding calls", e);
            }
        }
    }

    String contentHash(String text, String tenant) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(text.getBytes(StandardCharsets.UTF_8));
            digest.update(hashSuffix.getBytes(StandardCharsets.UTF_8));
            if (tenant != null && !tenant.isBlank()) {
                // the tenant lands in segment metadata, so it must invalidate the hash; absent
                // tenant adds nothing, keeping hashes of untenanted corpora stable
                digest.update(("\0" + tenant).getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String pipeline() {
        return pipeline;
    }

    /** Null in append mode. */
    public IngestionTracker tracker() {
        return tracker;
    }
}
