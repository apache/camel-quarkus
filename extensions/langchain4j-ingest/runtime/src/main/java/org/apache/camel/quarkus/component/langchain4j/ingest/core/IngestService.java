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
package org.apache.camel.quarkus.component.langchain4j.ingest.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.jboss.logging.Logger;

/**
 * Splits a document, embeds the segments and writes them to the store.
 *
 * <p>
 * Deliberately naive: it writes whatever it is given and remembers nothing, so ingesting a
 * document twice leaves two copies. Keeping a store in step with a changing source — skipping
 * unchanged documents, replacing changed ones, removing deleted ones — needs a record of what was
 * written, and that engine replaces this class.
 */
public class IngestService {

    private static final Logger LOG = Logger.getLogger(IngestService.class);

    public static final String METADATA_PIPELINE = "camel_quarkus_pipeline";
    public static final String METADATA_DOCUMENT_ID = "camel_quarkus_document_id";

    private static final int EMBEDDING_BATCH_SIZE = 32;

    private final String pipeline;
    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel model;
    private final DocumentSplitter splitter;

    public IngestService(String pipeline, EmbeddingStore<TextSegment> store, EmbeddingModel model,
            int maxSegmentSize, int maxOverlapSize) {
        this.pipeline = pipeline;
        this.store = store;
        this.model = model;
        this.splitter = DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize);
    }

    public IngestResult ingest(String documentId, String text) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("Ingestion pipeline '" + pipeline + "': documentId is required");
        }
        if (text == null || text.isBlank()) {
            return new IngestResult(pipeline, documentId, 0, IngestResult.Outcome.EMPTY);
        }

        // the document id travels with every segment: retrieval can cite it, and the engine that
        // replaces this one needs it to find a document's vectors again
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(METADATA_PIPELINE, pipeline);
        metadata.put(METADATA_DOCUMENT_ID, documentId);

        List<TextSegment> segments = splitter.split(Document.from(text, Metadata.from(metadata)));
        // embedded in batches: a single embedAll over a large document's full segment list can
        // exceed an embedding provider's per-request limits
        for (int from = 0; from < segments.size(); from += EMBEDDING_BATCH_SIZE) {
            List<TextSegment> batch = segments.subList(from, Math.min(from + EMBEDDING_BATCH_SIZE, segments.size()));
            store.addAll(model.embedAll(batch).content(), batch);
        }

        LOG.debugf("Ingestion pipeline '%s': wrote %d segment(s) of document '%s'", pipeline, segments.size(),
                documentId);
        return new IngestResult(pipeline, documentId, segments.size(), IngestResult.Outcome.INGESTED);
    }

    public String pipeline() {
        return pipeline;
    }
}
