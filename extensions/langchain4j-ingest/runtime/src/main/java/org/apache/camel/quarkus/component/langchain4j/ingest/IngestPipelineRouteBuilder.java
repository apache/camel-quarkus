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

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.langchain4j.ingest.IngestResult;
import org.apache.camel.component.langchain4j.ingest.LangChain4jIngest;
import org.apache.camel.component.langchain4j.ingest.LangChain4jIngestHeaders;
import org.apache.camel.component.langchain4j.ingest.TikaTextDecode;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.builder.ExpressionBuilder;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.URISupport;

/**
 * Generates one Camel route per {@link IngestPipelineDefinition}: consume, resolve the document id, optionally parse,
 * then split, embed and store through the {@code langchain4j-ingest} producer.
 *
 * <p>
 * A directory pipeline watches its folder with the safe file-consumer defaults — documents are left in place, unchanged
 * files are remembered in a duplicate register keyed on path, modification time and size, and a file still being copied
 * in is waited for. A consumer pipeline reads any component and deduplicates by document id when a repository is
 * configured. The id is always captured into an exchange property <em>before</em> the parse stage: a parser copies
 * document metadata over the headers, so a crafted document could otherwise forge its own identity.
 *
 * <p>
 * An internal copy of the pipeline assembly proposed alongside the upstream component: the delegation to Apache Camel
 * is engine-only, so the topology lives here, package-private — replaceable by an upstream artifact or kamelets if the
 * community adopts one of them.
 *
 * <p>
 * The class is abstract on purpose: camel-quarkus routes discovery instantiates every concrete public
 * {@code RouteBuilder} it finds reflectively, and an abstract base is skipped by construction.
 */
abstract class IngestPipelineRouteBuilder extends RouteBuilder {

    /**
     * The built-in register capacity, sized above Camel's 1000-entry default so eviction does not re-ingest large
     * directories during normal operation; in-memory, so lost on restart.
     */
    static final int DEFAULT_REGISTER_CAPACITY = 100_000;

    /**
     * The pipelines to build routes for; supplied by the subclass assembling its definitions from another source — a
     * configuration model, say.
     */
    protected abstract List<IngestPipelineDefinition> pipelines();

    @Override
    public void configure() {
        Set<String> names = new HashSet<>();
        for (IngestPipelineDefinition pipeline : pipelines()) {
            if (!names.add(pipeline.name())) {
                throw new IllegalArgumentException(
                        "Ingestion pipeline '" + pipeline.name() + "' is defined twice. Use one name per pipeline.");
            }
            configurePipeline(pipeline);
        }
    }

    private void configurePipeline(IngestPipelineDefinition pipeline) {
        requireParserComponent(pipeline);
        String storeRef = bindInstance(pipeline.embeddingStore(), pipeline.embeddingStoreRef(), pipeline, "store");
        String modelRef = bindInstance(pipeline.embeddingModel(), pipeline.embeddingModelRef(), pipeline, "model");
        String splitterRef = pipeline.documentSplitterRef();

        if (pipeline.directory() != null) {
            directoryRoute(pipeline, storeRef, modelRef, splitterRef);
            log.info("Ingestion pipeline '{}': source=file:{}", pipeline.name(), pipeline.directory());
        } else {
            consumerRoute(pipeline, storeRef, modelRef, splitterRef);
            log.info("Ingestion pipeline '{}': source={}", pipeline.name(), URISupport.sanitizeUri(pipeline.uri()));
        }
    }

    // ********************************************************************************
    // The two route topologies
    // ********************************************************************************

    /**
     * The route: watch the directory → resolve the id → (parse) → split, embed, store. The register in the file
     * endpoint keeps unchanged files from re-ingesting; an edited file gets a new key and re-ingests, its old segments
     * remain.
     */
    private void directoryRoute(IngestPipelineDefinition pipeline, String storeRef, String modelRef, String splitterRef) {
        String registerRef = repositoryRef(pipeline, true);
        Expression documentId = documentIdExpression(pipeline.documentId(), Exchange.FILE_NAME);

        ProcessorDefinition<?> route = from(fileEndpointUri(pipeline, registerRef))
                .routeId(routeId(pipeline))
                .setProperty(LangChain4jIngest.DOCUMENT_ID_PROPERTY, documentId);
        route = parseSteps(route, pipeline);
        // the file consumer discards the reply, so no repository is passed to the producer: the
        // endpoint register already keeps the same file version from being ingested twice
        ProcessorDefinition<?> tail = route.to(ingestEndpointUri(pipeline, storeRef, modelRef, splitterRef, null, null));
        if (pipeline.parser() != null) {
            // the discarded reply would otherwise hide it: a parse to nothing typically means a
            // missing Tika parser module or an image-only document, and the file's register key
            // is committed, so it is not retried until the file changes
            tail.process(exchange -> {
                IngestResult result = exchange.getMessage().getBody(IngestResult.class);
                if (result != null && result.outcome() == IngestResult.Outcome.EMPTY) {
                    log.warn("Ingestion pipeline '{}': document '{}' parsed to no text and was skipped; its key is"
                            + " committed, so it is not retried until the file changes (missing parser module?"
                            + " image-only document?)",
                            pipeline.name(), result.documentId());
                }
            });
        } else {
            // the discarded reply would otherwise hide even the trace of a blank file
            tail.process(exchange -> {
                IngestResult result = exchange.getMessage().getBody(IngestResult.class);
                if (result != null && result.outcome() == IngestResult.Outcome.EMPTY) {
                    log.debug("Ingestion pipeline '{}': document '{}' contained no text, nothing was written",
                            pipeline.name(), result.documentId());
                }
            });
        }
    }

    /** The route: consume → resolve the id → (parse) → split, embed, store; the reply is the result. */
    private void consumerRoute(IngestPipelineDefinition pipeline, String storeRef, String modelRef, String splitterRef) {
        String scheme = StringHelper.before(pipeline.uri(), ":");
        requireComponent(scheme, pipeline, "its source");
        String registerRef = repositoryRef(pipeline, false);
        Expression documentId = documentIdExpression(pipeline.documentId(), LangChain4jIngestHeaders.DOCUMENT_ID);

        ProcessorDefinition<?> route = from(pipeline.uri())
                .routeId(routeId(pipeline))
                .setProperty(LangChain4jIngest.DOCUMENT_ID_PROPERTY, documentId);
        route = parseSteps(route, pipeline);
        // deduplication by document id happens inside the producer, so a duplicate is answered
        // SKIPPED, a blank delivery releases its claim and a failed write can be retried
        // when the id comes from a plain header, the producer is told its name: the property set
        // above always wins, but the producer's missing-id error then names the header the route
        // actually reads instead of the component default
        String configured = pipeline.documentId();
        String documentIdHeader = configured != null && !configured.contains("${") && !configured.contains("$simple{")
                ? configured
                : null;
        route.to(ingestEndpointUri(pipeline, storeRef, modelRef, splitterRef, registerRef, documentIdHeader));
    }

    /**
     * The optional parse stage; the route is returned unchanged when the pipeline has no parser. Tika parses in-process
     * to its plain-text output — body subtree only, block boundaries newline-separated by Tika's own handlers, the
     * encoding pinned — and {@link TikaTextDecode} then decodes those bytes as the pinned charset, dodging the exchange
     * charset heuristic that the parsed document's own Content-Type header could steer. Docling hands the payload to a
     * Docling Serve instance and answers finished markdown; the body is pinned to bytes and the {@code CamelDocling*}
     * control headers are swept, because none of them may be decided by a consumer-delivered payload.
     */
    private ProcessorDefinition<?> parseSteps(ProcessorDefinition<?> route, IngestPipelineDefinition pipeline) {
        if (pipeline.parser() == null) {
            return route;
        }
        if (pipeline.maxDocumentSize() > 0) {
            // the endpoint's own cap counts extracted characters, which protects the splitter and
            // the model but not the parse: this guard rejects the raw payload first, before tika
            // or docling materialize it
            route = route.process(rawSizeGuard(pipeline));
        }
        return switch (pipeline.parser()) {
        case TIKA -> route.to("tika:parse?tikaParseOutputFormat=text&tikaParseOutputEncoding=UTF-8")
                .process(new TikaTextDecode());
        case DOCLING -> route.convertBodyTo(byte[].class)
                .removeHeaders("CamelDocling*")
                .to("docling:convert?operation=CONVERT_TO_MARKDOWN&contentInBody=true");
        };
    }

    private static Processor rawSizeGuard(IngestPipelineDefinition pipeline) {
        // the declared-length header spares even the read, but only the directory pipeline's own
        // file consumer is trusted to have set it: on a consumer pipeline every header may be
        // attacker-supplied along with the payload, so its body is always measured - a forged
        // CamelFileLength must not talk an oversized payload past the guard and into the parser
        boolean trustDeclaredLength = pipeline.directory() != null;
        return exchange -> {
            Long declared = trustDeclaredLength
                    ? exchange.getMessage().getHeader(Exchange.FILE_LENGTH, Long.class)
                    : null;
            long size;
            byte[] bounded = null;
            if (declared != null) {
                size = declared;
            } else if (exchange.getMessage().getBody() instanceof InputStream stream) {
                // bounded read: an attacker-sized stream is rejected after maxDocumentSize + 1
                // bytes instead of being materialized whole in the heap just to be measured;
                // an accepted stream is consumed here, so the bytes replace it as the body
                int limit = pipeline.maxDocumentSize() == Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : pipeline.maxDocumentSize() + 1;
                bounded = stream.readNBytes(limit);
                size = bounded.length;
            } else {
                // a null body carries no bytes to guard; it flows on and becomes the EMPTY outcome
                byte[] body = exchange.getMessage().getBody(byte[].class);
                size = body == null ? 0 : body.length;
            }
            if (size > pipeline.maxDocumentSize()) {
                throw new IllegalArgumentException(
                        "Ingestion pipeline '" + pipeline.name() + "': document '"
                                + exchange.getProperty(LangChain4jIngest.DOCUMENT_ID_PROPERTY,
                                        String.class)
                                + "' exceeds maxDocumentSize (" + size + " > "
                                + pipeline.maxDocumentSize() + " bytes)");
            }
            if (bounded != null) {
                exchange.getMessage().setBody(bounded);
            }
        };
    }

    // ********************************************************************************
    // Helper methods
    // ********************************************************************************

    /**
     * The directory consumer. Built through {@code createQueryString} rather than concatenated, so no option can be
     * injected through a crafted value. {@code noop} leaves the documents where they are, the register keeps the same
     * file version from being ingested twice, and the {@code changed} read lock waits for a file still being copied in.
     */
    private static String fileEndpointUri(IngestPipelineDefinition pipeline, String registerRef) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("noop", "true");
        options.put("idempotent", "true");
        options.put("idempotentRepository", "#bean:" + registerRef);
        // an edited file gets a new key and re-ingests; old segments remain (append)
        options.put("idempotentKey", "${file:absolute.path}:${file:modified}:${file:size}");
        options.put("recursive", String.valueOf(pipeline.recursive()));
        options.put("readLock", "changed");
        if (pipeline.parser() == null) {
            // text is read as UTF-8; a parser receives the raw bytes instead - the format is its
            // business, and a charset conversion would corrupt a binary document
            options.put("charset", "UTF-8");
        }
        return "file:" + pipeline.directory() + "?" + queryString(options);
    }

    private static String ingestEndpointUri(
            IngestPipelineDefinition pipeline, String storeRef, String modelRef, String splitterRef, String registerRef,
            String documentIdHeader) {
        Map<String, Object> options = new LinkedHashMap<>();
        if (documentIdHeader != null) {
            options.put("documentIdHeader", documentIdHeader);
        }
        options.put("maxSegmentSize", String.valueOf(pipeline.maxSegmentSize()));
        options.put("maxOverlapSize", String.valueOf(pipeline.maxOverlapSize()));
        options.put("embeddingBatchSize", String.valueOf(pipeline.embeddingBatchSize()));
        if (pipeline.maxDocumentSize() > 0) {
            options.put("maxDocumentSize", String.valueOf(pipeline.maxDocumentSize()));
        }
        if (splitterRef != null) {
            options.put("documentSplitter", "#bean:" + splitterRef);
        }
        if (storeRef != null) {
            options.put("embeddingStore", "#bean:" + storeRef);
        }
        if (modelRef != null) {
            options.put("embeddingModel", "#bean:" + modelRef);
        }
        if (registerRef != null) {
            options.put("idempotentRepository", "#bean:" + registerRef);
        }
        return LangChain4jIngest.SCHEME + ":" + pipeline.name() + "?" + queryString(options);
    }

    private static String queryString(Map<String, Object> options) {
        return URISupport.createQueryString(options);
    }

    /**
     * A bare header name is read as a header directly rather than parsed: a dotted header name would send the simple
     * parser into OGNL. A value containing {@code ${} or {@code $simple{} is a simple-language expression. The
     * expression is initialised here, at route build time — left to reify lazily it would race on the first concurrent
     * exchanges.
     */
    private Expression documentIdExpression(String configured, String defaultHeader) {
        Expression expression = configured == null
                ? ExpressionBuilder.headerExpression(defaultHeader)
                : configured.contains("${") || configured.contains("$simple{")
                        ? ExpressionBuilder.simpleExpression(configured)
                        : ExpressionBuilder.headerExpression(configured);
        expression.init(getContext());
        return expression;
    }

    /**
     * Resolves the pipeline's duplicate register to a registry reference: a named bean, an auto-created in-memory
     * register — the existing bean wins — or, when a directory pipeline names none, the built-in one.
     */
    private String repositoryRef(IngestPipelineDefinition pipeline, boolean required) {
        String ref = pipeline.idempotentRepositoryRef();
        if (pipeline.idempotentRepositoryAutoCreate()) {
            if (ref == null) {
                throw new IllegalStateException(
                        "Ingestion pipeline '" + pipeline.name() + "' sets idempotentRepositoryAutoCreate but no"
                                + " idempotentRepository name to create the register under.");
            }
            if (getContext().getRegistry().lookupByNameAndType(ref, IdempotentRepository.class) == null) {
                getContext().getRegistry().bind(ref,
                        MemoryIdempotentRepository.memoryIdempotentRepository(DEFAULT_REGISTER_CAPACITY));
            }
        }
        if (ref == null && required) {
            ref = generatedName(pipeline, "register");
            if (getContext().getRegistry().lookupByNameAndType(ref, IdempotentRepository.class) == null) {
                getContext().getRegistry().bind(ref,
                        MemoryIdempotentRepository.memoryIdempotentRepository(DEFAULT_REGISTER_CAPACITY));
            }
        }
        return ref;
    }

    /**
     * Binds a definition-supplied instance to the registry, so the endpoint can reference it;
     * called from {@code configure()}, which Camel runs single-threaded during startup.
     */
    private String bindInstance(Object instance, String ref, IngestPipelineDefinition pipeline, String what) {
        if (instance == null) {
            return ref;
        }
        String name = ref != null ? ref : generatedName(pipeline, what);
        getContext().getRegistry().bind(name, instance);
        return name;
    }

    private void requireParserComponent(IngestPipelineDefinition pipeline) {
        if (pipeline.parser() == null) {
            return;
        }
        switch (pipeline.parser()) {
        case TIKA -> requireComponent("tika", pipeline, "its parser");
        case DOCLING -> requireComponent("docling", pipeline, "its parser");
        }
    }

    /**
     * Fails a pipeline whose component is not on the classpath with the artifact to add, instead of Camel's bare
     * failed-to-resolve message much later.
     */
    private void requireComponent(String scheme, IngestPipelineDefinition pipeline, String purpose) {
        Component component = null;
        try {
            component = getContext().getComponent(scheme, true, false);
        } catch (Exception e) {
            // fall through to the actionable message
        }
        if (component == null) {
            throw new IllegalStateException(
                    "Ingestion pipeline '" + pipeline.name() + "' needs the '" + scheme + "' component for " + purpose
                            + ", which is not on the classpath. Add the extension that provides it, e.g. "
                            + "org.apache.camel.quarkus:camel-quarkus-" + scheme);
        }
    }

    private static String generatedName(IngestPipelineDefinition pipeline, String what) {
        return LangChain4jIngest.SCHEME + "-" + pipeline.name() + "-" + what;
    }

    private static String routeId(IngestPipelineDefinition pipeline) {
        return LangChain4jIngest.SCHEME + "-" + pipeline.name();
    }
}
