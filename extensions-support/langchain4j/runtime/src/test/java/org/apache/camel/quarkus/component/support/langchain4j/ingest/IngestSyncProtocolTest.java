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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.camel.quarkus.component.support.langchain4j.tracker.IngestionTracker;
import org.apache.camel.quarkus.component.support.langchain4j.tracker.jdbc.JdbcIngestionTracker;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sync protocol invariants, at the core level: skip tiers, replace without duplicates,
 * shrink, both write strategies, and crash convergence.
 */
class IngestSyncProtocolTest {

    JdbcDataSource dataSource;
    IngestionTracker tracker;
    UpsertFakeStore store;
    FakeModel model = new FakeModel();

    @BeforeEach
    void setUp() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        tracker = new JdbcIngestionTracker(dataSource);
        tracker.ensureSchema();
        store = new UpsertFakeStore();
    }

    /**
     * Strategy convention: tests that exit before the write protocol (gates, skip tiers, blank
     * content, dead-letter) or that write a first-ever document never reach a strategy-dependent
     * branch — they pass {@code UPSERT} as an arbitrary default. Behaviours that genuinely fork
     * per strategy (replace, shrink, crash convergence) are parameterized over both strategies
     * via {@code @EnumSource} instead. If the strategy ever gets consulted earlier than the
     * write protocol, this boundary must move with it.
     */
    IngestService syncService(IngestService.WriteStrategy strategy, EmbeddingStore<TextSegment> targetStore) {
        return new IngestService("p", targetStore, model, "none", 500, 50, tracker, strategy, "model-1");
    }

    @Test
    void ingestThenUnchangedFingerprintSkipsWithoutStoreAccess() {
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);

        IngestResult first = service.ingest("doc", "fp1", "hello world");
        assertEquals(IngestResult.Outcome.INGESTED, first.outcome());
        assertEquals(1, store.entries.size());
        int writesAfterFirst = store.writeCalls;

        IngestResult second = service.ingest("doc", "fp1", "hello world");
        assertEquals(IngestResult.Outcome.SKIPPED_UNCHANGED, second.outcome());
        assertEquals(writesAfterFirst, store.writeCalls, "tier-1 skip must not touch the store");
    }

    @Test
    void changedFingerprintSameContentSkipsAndRefreshesFingerprint() {
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);
        service.ingest("doc", "fp1", "hello world");

        IngestResult second = service.ingest("doc", "fp2", "hello world");
        assertEquals(IngestResult.Outcome.SKIPPED_UNCHANGED, second.outcome());
        assertEquals("fp2", tracker.read("p", "doc").orElseThrow().fingerprint(),
                "tier-2 skip must refresh the fingerprint so tier-1 works next time");

        int writes = store.writeCalls;
        IngestResult third = service.ingest("doc", "fp2", "hello world");
        assertEquals(IngestResult.Outcome.SKIPPED_UNCHANGED, third.outcome());
        assertEquals(writes, store.writeCalls);
    }

    /** The behaviours below differ per write strategy, so each runs under both. */
    static UpsertFakeStore storeFor(IngestService.WriteStrategy strategy) {
        // each strategy is paired with the store behaviour it exists for: upsert with a store
        // whose same-id write overwrites, remove-then-add with one whose same-id write duplicates
        return strategy == IngestService.WriteStrategy.UPSERT ? new UpsertFakeStore() : new DuplicatingFakeStore();
    }

    @ParameterizedTest
    @EnumSource(IngestService.WriteStrategy.class)
    void changedContentReplacesInPlaceWithoutDuplicates(IngestService.WriteStrategy strategy) {
        UpsertFakeStore target = storeFor(strategy);
        IngestService service = syncService(strategy, target);
        service.ingest("doc", "fp1", "old content");

        IngestResult result = service.ingest("doc", "fp2", "new content");
        assertEquals(IngestResult.Outcome.REPLACED, result.outcome());
        assertEquals(1, target.entries.size(), "the replacement must not duplicate");
        assertEquals("new content", target.entries.values().iterator().next().text());
    }

    @ParameterizedTest
    @EnumSource(IngestService.WriteStrategy.class)
    void shrinkRemovesStaleTailSegments(IngestService.WriteStrategy strategy) {
        UpsertFakeStore target = storeFor(strategy);
        IngestService service = new IngestService("p", target, model, "recursive", 12, 0, tracker,
                strategy, "model-1");

        service.ingest("doc", "fp1", "aaaa bbbb cccc dddd eeee");
        int before = target.entries.size();
        assertTrue(before > 1, "expected multiple segments, got " + before);

        IngestResult result = service.ingest("doc", "fp2", "tiny");
        assertEquals(IngestResult.Outcome.REPLACED, result.outcome());
        assertEquals(1, target.entries.size(), "stale tail segments must be removed on shrink");
    }

    @ParameterizedTest
    @EnumSource(IngestService.WriteStrategy.class)
    void crashAfterIntentConvergesOnNextDelivery(IngestService.WriteStrategy strategy) {
        UpsertFakeStore target = storeFor(strategy);
        IngestService service = syncService(strategy, target);
        service.ingest("doc", "fp1", "content v1");

        // simulate a crash: intent written (larger intended count), store partially written, no commit
        tracker.writeIntent("p", "doc", "fp2", "whatever", 5, IngestionTracker.ORIGIN_SOURCE);

        // same fingerprint as the crashed attempt: an in_progress row must never be skipped
        IngestResult result = service.ingest("doc", "fp2", "content v2");
        assertEquals(IngestResult.Outcome.REPLACED, result.outcome());
        assertTrue(tracker.read("p", "doc").orElseThrow().done());
        assertEquals(1, target.entries.size());
        assertEquals("content v2", target.entries.values().iterator().next().text());
    }

    @Test
    void deleteOfANeverIngestedDocumentRecordsTheSuppression() {
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);

        IngestResult deleted = service.delete("ghost");
        assertEquals(IngestResult.Outcome.DELETED, deleted.outcome());
        assertTrue(tracker.read("p", "ghost").orElseThrow().tombstone());

        // the suppression sticks: a source delivery of that id is refused
        IngestResult attempt = service.ingest("ghost", "fp1", "content");
        assertEquals(IngestResult.Outcome.SUPPRESSED_TOMBSTONE, attempt.outcome());
        assertEquals(0, store.entries.size());
    }

    @Test
    void goldenContentHashNeverChanges() {
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);
        // pins SCHEMA_VERSION, PARSER_ID, the NUL hash-domain separator and the suffix order
        // (model id "model-1", splitter "none", 500/50 from syncService): a change here re-embeds
        // every corpus on upgrade — it must be deliberate, never accidental
        assertEquals("f8fd87004690c8078456de954623d9202c7a4e75786d152e526650ea5c5c32eb",
                service.contentHash("hello world", null));
    }

    @Test
    void metadataIsWrittenOnEverySegment() {
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);
        service.ingest("doc", "fp1", "hello world", IngestService.Origin.SOURCE, "acme");

        TextSegment segment = store.entries.values().iterator().next();
        assertEquals("p", segment.metadata().getString(IngestService.METADATA_PIPELINE));
        assertEquals("doc", segment.metadata().getString(IngestService.METADATA_DOCUMENT_ID));
        assertEquals("acme", segment.metadata().getString(IngestService.METADATA_TENANT));
    }

    @Test
    void tenantChangeReplacesDespiteUnchangedContentAndFingerprint() {
        // single-strategy on purpose although it triggers a replacement: the pinned concern is
        // the skip-vs-replace DECISION, which is strategy-independent — the replacement
        // mechanics are covered per strategy by changedContentReplacesInPlaceWithoutDuplicates
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);
        service.ingest("doc", "fp1", "hello world", IngestService.Origin.SOURCE, "acme");

        IngestResult result = service.ingest("doc", "fp1", "hello world", IngestService.Origin.SOURCE, "globex");
        assertEquals(IngestResult.Outcome.REPLACED, result.outcome(),
                "the tenant lands in segment metadata, so a re-assignment must never be skipped");
        assertEquals("globex",
                store.entries.values().iterator().next().metadata().getString(IngestService.METADATA_TENANT));
    }

    @Test
    void blankContentKeepsThePreviousVersion() {
        // exits at the blank check, before the write protocol — strategy-independent (UPSERT is
        // the arbitrary default, see the syncService javadoc)
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);
        service.ingest("doc", "fp1", "good version");

        IngestResult result = service.ingest("doc", "fp2", "   ");
        assertEquals(IngestResult.Outcome.EMPTY, result.outcome());
        assertEquals(1, store.entries.size(), "a blank read must never empty the knowledge base");
        assertEquals("good version", store.entries.values().iterator().next().text());
    }

    @Test
    void appendModeWritesWithoutTracker() {
        UpsertFakeStore appendStore = new UpsertFakeStore();
        IngestService service = new IngestService("p", appendStore, model, "none", 500, 50,
                null, IngestService.WriteStrategy.UPSERT, "model-1");

        IngestResult result = service.ingest("doc", "some content");
        assertEquals(IngestResult.Outcome.INGESTED, result.outcome());
        assertEquals(1, appendStore.entries.size());
        assertNull(service.tracker());
    }

    @Test
    void transientFailureAfterIntentIsRetriedNotDeadLettered() {
        FailingModel failing = new FailingModel();
        IngestService service = new IngestService("p", store, failing, "none", 500, 50, tracker,
                IngestService.WriteStrategy.UPSERT, "model-1");
        service.ingest("doc", "fp1", "good version");

        failing.failNext = true;
        IngestService.RetryableIngestException e = assertThrows(IngestService.RetryableIngestException.class,
                () -> service.ingest("doc", "fp2", "new version"));
        assertTrue(e.committedVersionServes(), "the old generation is still in the store");
        assertFalse(tracker.read("p", "doc").orElseThrow().failed(),
                "an infrastructure failure must NOT dead-letter: the row stays in_progress and retries");
        assertEquals("good version", store.entries.values().iterator().next().text());

        IngestResult retry = service.ingest("doc", "fp2", "new version");
        assertEquals(IngestResult.Outcome.REPLACED, retry.outcome());
        assertEquals("new version", store.entries.values().iterator().next().text());
    }

    @Test
    void removeThenAddTransientFailureRecoversOnRetry() {
        DuplicatingFakeStore duplicating = new DuplicatingFakeStore();
        FailingModel failing = new FailingModel();
        IngestService service = new IngestService("p", duplicating, failing, "none", 500, 50, tracker,
                IngestService.WriteStrategy.REMOVE_THEN_ADD, "model-1");
        service.ingest("doc", "fp1", "good version");

        failing.failNext = true;
        IngestService.RetryableIngestException e = assertThrows(IngestService.RetryableIngestException.class,
                () -> service.ingest("doc", "fp2", "new version"));
        assertTrue(e.oldGenerationRemoved(), "the old generation was already removed when the write failed");
        assertEquals(0, duplicating.entriesList().size(), "the document is offline until a retry succeeds");
        assertFalse(tracker.read("p", "doc").orElseThrow().failed(),
                "a transient outage must stay retryable — dead-lettering it would drop the document forever");

        IngestResult retry = service.ingest("doc", "fp2", "new version");
        assertEquals(IngestResult.Outcome.REPLACED, retry.outcome());
        assertEquals("new version", duplicating.entriesList().get(0).text());
    }

    @Test
    void trackerBlipDuringWriteIntentIsRetriedNotDeadLettered() {
        BlippyTracker blippy = new BlippyTracker(dataSource);
        IngestService service = new IngestService("p", store, model, "none", 500, 50, blippy,
                IngestService.WriteStrategy.UPSERT, "model-1");
        service.ingest("doc", "fp1", "good version");

        blippy.failNextWriteIntent = true;
        assertThrows(IngestService.RetryableIngestException.class,
                () -> service.ingest("doc", "fp2", "new version"));
        assertFalse(blippy.read("p", "doc").orElseThrow().failed(),
                "a tracker blip is infrastructure — dead-lettering it would freeze the document");

        IngestResult retry = service.ingest("doc", "fp2", "new version");
        assertEquals(IngestResult.Outcome.REPLACED, retry.outcome());
        assertEquals("new version", store.entries.values().iterator().next().text());
    }

    @Test
    void tenantedPoisonDocumentIsDeadLetteredWithItsTenant() {
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);

        // first delivery of a tenanted document fails on read: the recorded gate must carry the
        // tenant, exactly like the stored fingerprint does, or it never matches again
        IngestService.ContentIngestException e = assertThrows(IngestService.ContentIngestException.class,
                () -> service.ingest("doc", "fp1", () -> {
                    throw new IllegalStateException("unreadable");
                }, IngestService.Origin.SOURCE, "acme"));
        tracker.markFailed("p", "doc", e.dedupFingerprint());

        IngestResult second = service.ingest("doc", "fp1", "now readable", IngestService.Origin.SOURCE, "acme");
        assertEquals(IngestResult.Outcome.DEAD_LETTERED, second.outcome(),
                "the same tenanted content must be recognized as already failed");
    }

    @Test
    void deadLetterFallsBackToTheContentHashForFingerprintlessSources() {
        IngestService service = syncService(IngestService.WriteStrategy.UPSERT, store);
        String hash = service.contentHash("poison content", null);
        tracker.markFailed("p", "doc", hash);

        IngestResult result = service.ingest("doc", null, "poison content");
        assertEquals(IngestResult.Outcome.DEAD_LETTERED, result.outcome(),
                "without a fingerprint the failed attempt's content hash gates the retry");
    }

    @Test
    void syncModeRequiresARemovalCapableStore() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> syncService(IngestService.WriteStrategy.UPSERT, new NoRemovalStore()));
        assertTrue(e.getMessage().contains("NoRemovalStore"), e.getMessage());
    }

    @Test
    void constructorGuardsMisconfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> new IngestService(" ", store, model, "none", 500, 50, null,
                        IngestService.WriteStrategy.UPSERT, "m"));
        assertThrows(IllegalArgumentException.class, () -> IngestService.WriteStrategy.of("merge"));

        IngestService append = new IngestService("p", store, model, "none", 500, 50, null,
                IngestService.WriteStrategy.UPSERT, "m");
        assertThrows(IllegalStateException.class, () -> append.delete("doc"),
                "sync-only operations must refuse in append mode");
        assertThrows(IllegalArgumentException.class, () -> append.ingest(" ", "content"));
    }

    // --- fakes -------------------------------------------------------------------------------

    /** Same-id write overwrites — the measured pgvector/qdrant/elasticsearch behaviour. */
    static class UpsertFakeStore implements EmbeddingStore<TextSegment> {
        final Map<String, TextSegment> entries = new LinkedHashMap<>();
        int writeCalls;

        @Override
        public void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> segments) {
            writeCalls++;
            for (int i = 0; i < ids.size(); i++) {
                entries.put(ids.get(i), segments.get(i));
            }
        }

        @Override
        public void removeAll(java.util.Collection<String> ids) {
            ids.forEach(entries::remove);
        }

        @Override
        public String add(Embedding embedding) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(String id, Embedding embedding) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String add(Embedding embedding, TextSegment segment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
            return new EmbeddingSearchResult<>(List.of());
        }
    }

    /** Same-id write appends another row — the chroma/milvus/in-memory behaviour. */
    static class DuplicatingFakeStore extends UpsertFakeStore {
        final List<TextSegment> rows = new ArrayList<>();
        final List<String> rowIds = new ArrayList<>();

        @Override
        public void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> segments) {
            writeCalls++;
            for (int i = 0; i < ids.size(); i++) {
                rowIds.add(ids.get(i));
                rows.add(segments.get(i));
            }
            rebuild();
        }

        @Override
        public void removeAll(java.util.Collection<String> ids) {
            for (int i = rowIds.size() - 1; i >= 0; i--) {
                if (ids.contains(rowIds.get(i))) {
                    rowIds.remove(i);
                    rows.remove(i);
                }
            }
            rebuild();
        }

        List<TextSegment> entriesList() {
            return rows;
        }

        private void rebuild() {
            entries.clear();
            for (int i = 0; i < rowIds.size(); i++) {
                // duplicate ids collapse in the map, so expose the raw rows for assertions
                entries.put(rowIds.get(i) + "#" + i, rows.get(i));
            }
        }
    }

    @Test
    void embeddingBatchSizeIsRespected() {
        FakeModel countingModel = new FakeModel();
        IngestService service = new IngestService("p", store, countingModel, "recursive", 12, 0, tracker,
                IngestService.WriteStrategy.UPSERT, "m1").embeddingLimits(2, null);

        service.ingest("doc", "fp1", "aaaa bbbb cccc dddd eeee");

        assertTrue(countingModel.batchSizes.size() > 1, "multiple batches expected");
        assertTrue(countingModel.batchSizes.stream().allMatch(size -> size <= 2),
                "no batch may exceed the configured size, got " + countingModel.batchSizes);
    }

    static class FakeModel implements EmbeddingModel {
        final List<Integer> batchSizes = new ArrayList<>();

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            batchSizes.add(segments.size());
            List<Embedding> out = new ArrayList<>(segments.size());
            for (TextSegment segment : segments) {
                out.add(embeddingFor(segment.text()));
            }
            return Response.from(out);
        }

        static Embedding embeddingFor(String text) {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
                Random random = new Random(digest[0]);
                float[] vector = new float[8];
                for (int i = 0; i < vector.length; i++) {
                    vector[i] = random.nextFloat();
                }
                return new Embedding(vector);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Delegates to the real JDBC tracker, failing one writeIntent on demand — a pool blip. */
    static class BlippyTracker extends JdbcIngestionTracker {
        boolean failNextWriteIntent;

        BlippyTracker(javax.sql.DataSource dataSource) {
            super(dataSource);
        }

        @Override
        public void writeIntent(String pipeline, String documentId, String fingerprint, String contentHash,
                int intendedCount, String origin) {
            if (failNextWriteIntent) {
                failNextWriteIntent = false;
                throw new IllegalStateException("simulated connection pool blip");
            }
            super.writeIntent(pipeline, documentId, fingerprint, contentHash, intendedCount, origin);
        }
    }

    /** Fails the next embedAll call, then recovers — a transient provider outage. */
    static class FailingModel extends FakeModel {
        boolean failNext;

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("simulated provider outage (503)");
            }
            return super.embedAll(segments);
        }
    }

    /** Writes work, id-addressed removal does not — the shape of cassandra/astra-db. */
    static class NoRemovalStore extends UpsertFakeStore {
        @Override
        public void removeAll(java.util.Collection<String> ids) {
            throw new UnsupportedOperationException("Not supported");
        }
    }
}
