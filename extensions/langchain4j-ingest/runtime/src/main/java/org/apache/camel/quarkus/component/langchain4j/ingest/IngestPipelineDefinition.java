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

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.camel.builder.EndpointConsumerBuilder;

/**
 * A declarative description of one ingestion pipeline, turned into a route by {@link IngestPipelineRouteBuilder}: what
 * to consume, how to identify each document, whether to parse it, and where to embed and store it.
 *
 * <p>
 * A pipeline reads either a directory ({@link #directory(String, String)}) or any Camel consumer
 * ({@link #consumer(String, String)}); every other property has a matching endpoint option on the
 * {@code langchain4j-ingest} producer the generated route ends in.
 *
 * <p>
 * An internal copy of the pipeline assembly proposed alongside the upstream component — see
 * {@link IngestPipelineRouteBuilder}.
 */
final class IngestPipelineDefinition {

    /** The supported document parsers; the configuration value is the lower-case name. */
    public enum Parser {
        TIKA,
        DOCLING;

        static Parser of(String value) {
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "parser must be one of " + SUPPORTED_PARSERS + " (got '" + value + "')");
            }
        }
    }

    // derived from the enum, so a parser added there cannot diverge from the validated set
    public static final Set<String> SUPPORTED_PARSERS = Arrays.stream(Parser.values())
            .map(parser -> parser.name().toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    private final String name;
    private final String directory;
    private final String uri;

    private boolean recursive = true;
    private String documentId;
    private Parser parser;
    private int maxSegmentSize = IngestBuildTimeConfig.DEFAULT_MAX_SEGMENT_SIZE;
    private int maxOverlapSize = IngestBuildTimeConfig.DEFAULT_MAX_OVERLAP_SIZE;
    private int embeddingBatchSize = IngestBuildTimeConfig.DEFAULT_EMBEDDING_BATCH_SIZE;
    private int maxDocumentSize = IngestBuildTimeConfig.DEFAULT_MAX_DOCUMENT_SIZE;

    private String embeddingStoreRef;
    private EmbeddingStore<TextSegment> embeddingStore;
    private String embeddingModelRef;
    private EmbeddingModel embeddingModel;
    private String documentSplitterRef;

    private String idempotentRepositoryRef;
    private boolean idempotentRepositoryAutoCreate;

    private IngestPipelineDefinition(String name, String directory, String uri) {
        this.name = name;
        this.directory = directory;
        this.uri = uri;
    }

    /**
     * A pipeline reading the files of a directory: they are left in place (a knowledge base reads its source, it does
     * not consume it), unchanged files are remembered and skipped, and a file still being copied in is waited for
     * rather than half-ingested.
     */
    public static IngestPipelineDefinition directory(String name, String directory) {
        requireText(directory, "directory");
        if (directory.contains("?") || directory.contains("#")) {
            throw new IllegalArgumentException(
                    "Ingestion pipeline '" + name + "': the directory must not contain '?' or '#' (got '"
                            + directory + "')");
        }
        return new IngestPipelineDefinition(requireName(name), directory, null);
    }

    /**
     * A pipeline consuming from any Camel component, with the component's own options — the escape hatch beside
     * {@link #directory(String, String)}. Which part of the exchange identifies the document is the consumer's
     * business, so say it with {@link #documentId}: {@code CamelAwsS3Key} for an S3 consumer, {@code CamelKafkaKey} for
     * a Kafka one.
     */
    public static IngestPipelineDefinition consumer(String name, String uri) {
        requireText(uri, "uri");
        if (!uri.contains(":")) {
            throw new IllegalArgumentException(
                    "Ingestion pipeline '" + name + "': '" + uri + "' is not a consumer URI");
        }
        return new IngestPipelineDefinition(requireName(name), null, uri);
    }

    /**
     * The same, built with the Camel Endpoint DSL (add the {@code camel-endpointdsl} dependency and use its static
     * builders or an {@code EndpointBuilderFactory}).
     */
    public static IngestPipelineDefinition consumer(String name, EndpointConsumerBuilder uri) {
        return consumer(name, uri.getRawUri());
    }

    /** Whether subdirectories are ingested too; directory pipelines only. */
    public IngestPipelineDefinition recursive(boolean recursive) {
        this.recursive = recursive;
        return this;
    }

    /**
     * Where the document id lives in the exchange the consumer delivers: the name of a header, or a simple-language
     * expression (a value containing {@code ${} or {@code $simple{} is parsed as one). When not set, a directory
     * pipeline uses the file name and a consumer pipeline the {@code CamelLangChain4jIngestDocumentId} header. The id
     * is captured before any parse, so a document cannot forge its own identity.
     */
    public IngestPipelineDefinition documentId(String documentId) {
        this.documentId = requireText(documentId, "documentId");
        return this;
    }

    /**
     * Parses the consumed payload into text before splitting: {@code tika} extracts plain text in-process,
     * {@code docling} converts to markdown through a Docling Serve instance. The corresponding component must be on the
     * classpath: {@code camel-tika} or {@code camel-docling}.
     */
    public IngestPipelineDefinition parser(String parser) {
        this.parser = Parser.of(requireText(parser, "parser"));
        return this;
    }

    /**
     * The typed twin of {@link #parser(String)}, for Java callers; the String form exists for configuration
     * translators, whose input is text by nature.
     */
    public IngestPipelineDefinition parser(Parser parser) {
        if (parser == null) {
            throw new IllegalArgumentException("Ingestion pipeline parser must not be null");
        }
        this.parser = parser;
        return this;
    }

    /** The splitter bounds; the same rule the endpoint enforces, failing early here. */
    public IngestPipelineDefinition splitter(int maxSegmentSize, int maxOverlapSize) {
        if (maxSegmentSize <= 0 || maxOverlapSize < 0 || maxOverlapSize >= maxSegmentSize) {
            throw new IllegalArgumentException(
                    "Ingestion pipeline '" + name + "': maxSegmentSize must be positive and maxOverlapSize must be"
                            + " non-negative and smaller than it (got " + maxSegmentSize + " / "
                            + maxOverlapSize + ")");
        }
        this.maxSegmentSize = maxSegmentSize;
        this.maxOverlapSize = maxOverlapSize;
        return this;
    }

    /**
     * How many segments are embedded per request to the embedding model; the same rule the endpoint enforces, failing
     * early here. A batch carries at most {@code embeddingBatchSize x maxSegmentSize} characters, so tune the two
     * together against the provider's token limits.
     */
    public IngestPipelineDefinition embeddingBatchSize(int embeddingBatchSize) {
        if (embeddingBatchSize < 1) {
            throw new IllegalArgumentException(
                    "Ingestion pipeline '" + name + "': embeddingBatchSize must be positive (got "
                            + embeddingBatchSize + ")");
        }
        this.embeddingBatchSize = embeddingBatchSize;
        return this;
    }

    /**
     * Maximum size of one document; unset means no limit. The pipeline holds a document in memory whole, so the cap is
     * the protection against oversized — on a consumer-fed pipeline, attacker-sized — payloads. It is enforced twice: a
     * pipeline with a {@link #parser(Parser)} rejects a larger raw payload (in bytes) before the parse ever runs, and
     * the endpoint rejects longer extracted text (in characters) before splitting — a small file can still parse into a
     * lot of text. An oversized document fails the exchange cleanly: a directory pipeline quarantines it like any other
     * failure, a consumer pipeline propagates the error to its source, and a dedup claim is released either way.
     */
    public IngestPipelineDefinition maxDocumentSize(int maxDocumentSize) {
        if (maxDocumentSize < 0) {
            throw new IllegalArgumentException(
                    "Ingestion pipeline '" + name + "': maxDocumentSize must not be negative, 0 meaning no limit (got "
                            + maxDocumentSize + ")");
        }
        this.maxDocumentSize = maxDocumentSize;
        return this;
    }

    /**
     * Name of the {@code DocumentSplitter} bean replacing the default recursive splitting; {@code maxSegmentSize} and
     * {@code maxOverlapSize} are then ignored. Referenced by name on purpose — an application may hold unrelated
     * splitters, so none is picked up by type. Segments returned without the identity metadata are re-stamped, so a
     * custom splitter cannot break citation.
     */
    public IngestPipelineDefinition documentSplitter(String beanName) {
        this.documentSplitterRef = requireText(beanName, "documentSplitter");
        return this;
    }

    /** Name of the {@code EmbeddingStore} bean to write to; omitted, the single bean of the type is used. */
    public IngestPipelineDefinition embeddingStore(String beanName) {
        this.embeddingStoreRef = requireText(beanName, "embeddingStore");
        return this;
    }

    /** The {@code EmbeddingStore} to write to, as an instance; it is bound to the registry under a generated name. */
    public IngestPipelineDefinition embeddingStore(EmbeddingStore<TextSegment> store) {
        this.embeddingStore = store;
        return this;
    }

    /** Name of the {@code EmbeddingModel} bean to embed with; omitted, the single bean of the type is used. */
    public IngestPipelineDefinition embeddingModel(String beanName) {
        this.embeddingModelRef = requireText(beanName, "embeddingModel");
        return this;
    }

    /** The {@code EmbeddingModel} to embed with, as an instance; it is bound to the registry under a generated name. */
    public IngestPipelineDefinition embeddingModel(EmbeddingModel model) {
        this.embeddingModel = model;
        return this;
    }

    /**
     * Name of the {@code IdempotentRepository} bean remembering ingested documents. A directory pipeline uses it in its
     * file consumer, keyed on path, modification time and size, so a persistent repository makes restarts free. A
     * consumer pipeline deduplicates deliveries by document id: first write wins.
     */
    public IngestPipelineDefinition idempotentRepository(String beanName) {
        this.idempotentRepositoryRef = requireText(beanName, "idempotentRepository");
        return this;
    }

    /**
     * When {@code true}, an in-memory register (100 000 keys, lost on restart) is created and bound under the
     * {@link #idempotentRepository(String)} name, unless a bean with that name exists — the existing bean wins.
     */
    public IngestPipelineDefinition idempotentRepositoryAutoCreate(boolean autoCreate) {
        this.idempotentRepositoryAutoCreate = autoCreate;
        return this;
    }

    private static String requireName(String name) {
        requireText(name, "name");
        if (!name.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "Ingestion pipeline name '" + name + "' may only contain letters, digits, '.', '_' and '-'");
        }
        return name;
    }

    private static String requireText(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ingestion pipeline " + what + " must not be null or blank");
        }
        return value;
    }

    String name() {
        return name;
    }

    String directory() {
        return directory;
    }

    String uri() {
        return uri;
    }

    boolean recursive() {
        return recursive;
    }

    String documentId() {
        return documentId;
    }

    Parser parser() {
        return parser;
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

    String documentSplitterRef() {
        return documentSplitterRef;
    }

    String embeddingStoreRef() {
        return embeddingStoreRef;
    }

    EmbeddingStore<TextSegment> embeddingStore() {
        return embeddingStore;
    }

    String embeddingModelRef() {
        return embeddingModelRef;
    }

    EmbeddingModel embeddingModel() {
        return embeddingModel;
    }

    String idempotentRepositoryRef() {
        return idempotentRepositoryRef;
    }

    boolean idempotentRepositoryAutoCreate() {
        return idempotentRepositoryAutoCreate;
    }
}
