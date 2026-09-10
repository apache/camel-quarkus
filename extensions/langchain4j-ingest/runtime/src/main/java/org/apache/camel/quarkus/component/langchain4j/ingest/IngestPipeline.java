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

import java.util.Optional;
import java.util.Set;

/**
 * A pipeline declared in Java rather than in configuration, returned from an {@link Ingest}
 * method. Every property has a configuration twin, and both paths share the same runtime.
 */
public final class IngestPipeline {

    /** The values {@link #parser(String)} and the {@code parser} configuration property accept. */
    public static final Set<String> SUPPORTED_PARSERS = IngestPipelineDefinition.SUPPORTED_PARSERS;

    private final Source source;
    private String embeddingStoreName;
    private String embeddingModelName;
    private String parser;
    private int maxSegmentSize = IngestBuildTimeConfig.DEFAULT_MAX_SEGMENT_SIZE;
    private int maxOverlapSize = IngestBuildTimeConfig.DEFAULT_MAX_OVERLAP_SIZE;
    private int embeddingBatchSize = IngestBuildTimeConfig.DEFAULT_EMBEDDING_BATCH_SIZE;
    private int maxDocumentSize = IngestBuildTimeConfig.DEFAULT_MAX_DOCUMENT_SIZE;
    private String documentSplitterName;

    private IngestPipeline(Source source) {
        this.source = source;
    }

    public static IngestPipeline from(Source source) {
        return new IngestPipeline(source);
    }

    public IngestPipeline embeddingStore(String beanName) {
        this.embeddingStoreName = beanName;
        return this;
    }

    public IngestPipeline embeddingModel(String beanName) {
        this.embeddingModelName = beanName;
        return this;
    }

    /**
     * Parses the consumed payload into text before splitting: {@code tika} extracts plain text
     * in-process, {@code docling} converts to markdown through a Docling Serve instance. The
     * corresponding extension must be on the classpath; the twin of the {@code parser}
     * configuration property.
     */
    public IngestPipeline parser(String parser) {
        // the same rule the configuration path is held to at build time; the null check comes
        // first because the unmodifiable set's contains(null) throws a bare NPE
        if (parser == null || !SUPPORTED_PARSERS.contains(parser)) {
            throw new IllegalArgumentException("parser must be one of " + SUPPORTED_PARSERS
                    + " (got '" + parser + "')");
        }
        this.parser = parser;
        return this;
    }

    public IngestPipeline splitter(int maxSegmentSize, int maxOverlapSize) {
        // the same rule the configuration path is held to at build time, so both paths reject
        // the same values rather than failing later inside the splitter
        if (maxSegmentSize <= 0 || maxOverlapSize < 0 || maxOverlapSize >= maxSegmentSize) {
            throw new IllegalArgumentException("max-segment-size must be positive and max-overlap-size must be "
                    + "smaller than it (got " + maxSegmentSize + " / " + maxOverlapSize + ")");
        }
        this.maxSegmentSize = maxSegmentSize;
        this.maxOverlapSize = maxOverlapSize;
        return this;
    }

    /**
     * How many segments are embedded per request to the embedding model; the twin of the
     * {@code embedding-batch-size} configuration property.
     */
    public IngestPipeline embeddingBatchSize(int embeddingBatchSize) {
        // the same rule the configuration path is held to at build time
        if (embeddingBatchSize < 1) {
            throw new IllegalArgumentException(
                    "embedding-batch-size must be positive (got " + embeddingBatchSize + ")");
        }
        this.embeddingBatchSize = embeddingBatchSize;
        return this;
    }

    /**
     * Maximum size of one document in characters; 0, the default, means no limit. The twin of
     * the {@code max-document-size} configuration property.
     */
    public IngestPipeline maxDocumentSize(int maxDocumentSize) {
        // the same rule the configuration path is held to at build time
        if (maxDocumentSize < 0) {
            throw new IllegalArgumentException(
                    "max-document-size must not be negative, 0 meaning no limit (got " + maxDocumentSize + ")");
        }
        this.maxDocumentSize = maxDocumentSize;
        return this;
    }

    /**
     * Name of the {@code DocumentSplitter} bean replacing the default recursive splitting; the
     * twin of the {@code document-splitter} configuration property. The splitter sizes are then
     * ignored.
     */
    public IngestPipeline documentSplitter(String beanName) {
        this.documentSplitterName = beanName;
        return this;
    }

    String sourceType() {
        return source.type();
    }

    String sourceUri() {
        return source.uri();
    }

    Optional<String> embeddingStoreName() {
        return Optional.ofNullable(embeddingStoreName);
    }

    Optional<String> embeddingModelName() {
        return Optional.ofNullable(embeddingModelName);
    }

    Optional<String> parser() {
        return Optional.ofNullable(parser);
    }

    int maxSegmentSize() {
        return maxSegmentSize;
    }

    int maxOverlapSize() {
        return maxOverlapSize;
    }

    int embeddingBatchSize() {
        return embeddingBatchSize;
    }

    int maxDocumentSize() {
        return maxDocumentSize;
    }

    Optional<String> documentSplitterName() {
        return Optional.ofNullable(documentSplitterName);
    }

    /** The configuration view, so a builder pipeline reuses every configuration path verbatim. */
    IngestRunTimeConfig.PipelineRunTimeConfig asRunTimeConfig() {
        IngestRunTimeConfig.PipelineRunTimeConfig.SourceRunTimeConfig sourceConfig = source.asRunTimeConfig();
        return new IngestRunTimeConfig.PipelineRunTimeConfig() {

            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public SourceRunTimeConfig source() {
                return sourceConfig;
            }
        };
    }
}
