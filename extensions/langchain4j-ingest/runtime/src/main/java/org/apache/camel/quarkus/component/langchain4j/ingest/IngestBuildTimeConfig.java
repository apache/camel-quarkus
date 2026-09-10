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
package org.apache.camel.quarkus.component.langchain4j.ingest;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * The shape of an ingestion pipeline: what it reads and where it writes. Locations that differ
 * per deployment are runtime configuration, see {@link IngestRunTimeConfig}.
 */
@ConfigMapping(prefix = "quarkus.camel.langchain4j.ingest")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface IngestBuildTimeConfig {

    /** Mirrors the {@code @WithDefault} below, which can only carry a literal. */
    int DEFAULT_MAX_SEGMENT_SIZE = 500;

    /** Mirrors the {@code @WithDefault} below, which can only carry a literal. */
    int DEFAULT_MAX_OVERLAP_SIZE = 50;

    /** Mirrors the {@code @WithDefault} below, which can only carry a literal. */
    int DEFAULT_EMBEDDING_BATCH_SIZE = 32;

    /** Mirrors the {@code @WithDefault} below, which can only carry a literal; 0 means no limit. */
    int DEFAULT_MAX_DOCUMENT_SIZE = 0;

    /**
     * Ingestion pipelines by name.
     */
    @WithParentName
    @ConfigDocMapKey("pipeline-name")
    Map<String, PipelineBuildTimeConfig> pipelines();

    interface PipelineBuildTimeConfig {

        /**
         * The document source.
         */
        SourceBuildTimeConfig source();

        /**
         * How a consumed payload becomes text before it is split: `tika` extracts plain text
         * from PDF, office and similar formats in-process, `docling` converts to
         * structure-preserving markdown through a Docling Serve instance (configure it with
         * `camel.component.docling.*`). When not set, the payload is read as text as-is. Each
         * value needs its extension on the classpath: `camel-quarkus-tika` or
         * `camel-quarkus-docling`.
         */
        Optional<String> parser();

        /**
         * Name of the `EmbeddingStore` bean to write to. When not set, the only one present is
         * used.
         */
        Optional<String> embeddingStore();

        /**
         * Name of the `EmbeddingModel` bean to embed with. When not set, the only one present is
         * used.
         */
        Optional<String> embeddingModel();

        /**
         * Maximum size of one segment, in characters.
         */
        @WithDefault("500")
        int maxSegmentSize();

        /**
         * How much of the previous segment each segment repeats, in characters. Overlap keeps a
         * sentence split across a boundary retrievable from either side.
         */
        @WithDefault("50")
        int maxOverlapSize();

        /**
         * How many segments are embedded per request to the embedding model. Providers with
         * generous per-request limits ingest large documents faster with a bigger batch; a batch
         * carries at most `embedding-batch-size` × `max-segment-size` characters, so tune the two
         * together against the provider's token limits.
         */
        @WithDefault("32")
        int embeddingBatchSize();

        /**
         * Maximum size of one document in characters, applied to the text about to be split;
         * 0, the default, means no limit. The pipeline holds a document in memory whole, so the
         * cap is the protection against oversized — on a consumer-fed pipeline, attacker-sized —
         * payloads. An oversized document fails the exchange cleanly.
         */
        @WithDefault("0")
        int maxDocumentSize();

        /**
         * Name of the `DocumentSplitter` bean replacing the default recursive splitting;
         * `max-segment-size` and `max-overlap-size` are then ignored. Looked up by name only —
         * an application may hold unrelated splitters. Segments returned without the identity
         * metadata are re-stamped, so a custom splitter cannot break citation.
         */
        Optional<String> documentSplitter();

        /**
         * A pipeline reads either a directory or a Camel consumer, and the two halves of that
         * choice deliberately sit in different config roots: the consumer URI is fixed at build
         * time, while the directory is a location that changes per deployment and so lives in
         * {@link IngestRunTimeConfig.PipelineRunTimeConfig.SourceRunTimeConfig} along with the
         * rest of the per-deployment source settings.
         */
        interface SourceBuildTimeConfig {

            /**
             * The Camel consumer URI feeding this pipeline: any component, with its own options.
             * Setting it is what makes the pipeline consume from that component; leaving it unset
             * makes the pipeline read the directory named by the runtime `source.directory`
             * property instead. Fixed at build time by design — a runtime-overridable consumer
             * URI would be arbitrary component invocation. Property placeholders inside it still
             * resolve at startup, so credentials and endpoints remain runtime configuration.
             */
            Optional<String> uri();
        }
    }
}
