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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.langchain4j.ingest.IngestResult;
import org.apache.camel.component.langchain4j.ingest.LangChain4jIngest;
import org.apache.camel.component.langchain4j.ingest.LangChain4jIngestHeaders;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.builder.ExpressionBuilder;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.util.URISupport;
import org.jboss.logging.Logger;

/**
 * Translates the extension's configuration model — build-time and runtime properties and
 * {@code @Ingest} builder methods — into one route per pipeline, composed from the catalog's
 * langchain4j-ingest Kamelets, each adding a route named after the pipeline. Their engine
 * lives in the {@code camel-langchain4j-ingest} component. What stays here is the Quarkus DX:
 * CDI bean resolution with its actionable messages, and the configuration-level validations.
 */
@ApplicationScoped
public class IngestRoutes extends RouteBuilder {

    /**
     * The built-in register capacity, sized above Camel's 1000-entry default so eviction does
     * not re-ingest large directories during normal operation; in-memory, so lost on restart.
     */
    static final int DEFAULT_REGISTER_CAPACITY = 100_000;

    // derived from the enum, so a parser added there cannot diverge from the validated set
    static final Set<String> SUPPORTED_PARSERS = Arrays.stream(Parser.values())
            .map(parser -> parser.name().toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    private static final String FILE_SOURCE_KAMELET = "langchain4j-ingest-file-source";
    private static final String SINK_KAMELET = "langchain4j-ingest-sink";

    /** Every catalog Kamelet a pipeline can use, by id; the deployment module embeds these in a native image. */
    public static final List<String> KAMELETS = Stream.concat(Stream.of(FILE_SOURCE_KAMELET, SINK_KAMELET),
            Arrays.stream(Parser.values()).map(parser -> parser.actionKamelet))
            .toList();

    private static final Logger LOG = Logger.getLogger(IngestRoutes.class);

    @Inject
    IngestBuildTimeConfig buildTimeConfig;

    @Inject
    IngestRunTimeConfig runTimeConfig;

    @Inject
    IngestBuilderPipelines builderPipelines;

    // these injection points also keep an unnamed store or model bean from being removed as
    // unused - nothing else in the application need inject it
    @Inject
    @Any
    Instance<EmbeddingStore<TextSegment>> storeCandidates;

    @Inject
    @Any
    Instance<EmbeddingModel> modelCandidates;

    @Override
    public void configure() {
        // a pipeline may be declared entirely through runtime properties - the documented
        // minimum is a directory and nothing else - so the two config roots are unioned. Keying
        // off the build-time map alone would make that configuration a silent no-op, since
        // SmallRye only materialises a map key for the mapping whose structure a property matches
        Set<String> builderDeclared = builderPipelines.entries().stream()
                .map(IngestBuilderPipelines.Entry::name)
                .collect(Collectors.toSet());
        Set<String> names = new TreeSet<>(buildTimeConfig.pipelines().keySet());
        names.addAll(runTimeConfig.pipelines().keySet());
        names.removeAll(builderDeclared);
        rejectRouteIdClashes(names, builderDeclared);

        for (String name : names) {
            IngestBuildTimeConfig.PipelineBuildTimeConfig pipeline = buildTimeConfig.pipelines().get(name);
            IngestRunTimeConfig.PipelineRunTimeConfig runtime = runTimeConfig.pipelines().get(name);

            if (runtime != null && !runtime.enabled()) {
                LOG.infof("Ingestion pipeline '%s' is disabled", name);
                continue;
            }

            // a consumer URI says "consume from this"; its absence says "read that directory"
            String uri = pipeline == null ? null : pipeline.source().uri().orElse(null);
            if (uri != null && runtime != null && runtime.source().directory().isPresent()) {
                throw new IllegalStateException("Ingestion pipeline '" + name + "' sets both source.uri ('" + uri
                        + "') and source.directory ('" + runtime.source().directory().get() + "'). A pipeline "
                        + "reads one source: keep the URI, or drop it to read the directory.");
            }

            compositionRoute(name, uri, runtime,
                    resolveStore(name, pipeline == null ? null : pipeline.embeddingStore().orElse(null)),
                    resolveModel(name, pipeline == null ? null : pipeline.embeddingModel().orElse(null)),
                    pipeline == null ? IngestBuildTimeConfig.DEFAULT_MAX_SEGMENT_SIZE : pipeline.maxSegmentSize(),
                    pipeline == null ? IngestBuildTimeConfig.DEFAULT_MAX_OVERLAP_SIZE : pipeline.maxOverlapSize(),
                    pipeline == null ? IngestBuildTimeConfig.DEFAULT_EMBEDDING_BATCH_SIZE : pipeline.embeddingBatchSize(),
                    pipeline == null ? IngestBuildTimeConfig.DEFAULT_MAX_DOCUMENT_SIZE : pipeline.maxDocumentSize(),
                    pipeline == null ? null : pipeline.documentSplitter().orElse(null),
                    pipeline == null ? null : pipeline.parser().orElse(null));
        }

        for (IngestBuilderPipelines.Entry entry : builderPipelines.entries()) {
            builderPipeline(entry);
        }
    }

    /**
     * A pipeline named after a step of another one - {@code x} and {@code x-source}, {@code x-parser} or
     * {@code x-sink} - would share a route id with it; both are named here, before any route exists, instead
     * of the obscure failure Camel reports for the second route. A disabled pipeline adds no route.
     */
    private void rejectRouteIdClashes(Set<String> configured, Set<String> builderDeclared) {
        Set<String> enabled = new TreeSet<>();
        Stream.concat(configured.stream(), builderDeclared.stream())
                .filter(name -> {
                    IngestRunTimeConfig.PipelineRunTimeConfig runtime = runTimeConfig.pipelines().get(name);
                    return runtime == null || runtime.enabled();
                })
                .forEach(enabled::add);
        for (String name : enabled) {
            for (String step : List.of("source", "parser", "sink")) {
                String other = name + "-" + step;
                if (enabled.contains(other)) {
                    throw new IllegalStateException("Ingestion pipelines '" + name + "' and '" + other
                            + "' would both use the route id 'langchain4j-ingest-" + other + "'. Rename one of them.");
                }
            }
        }
    }

    /** An {@code @Ingest}-declared pipeline: the builder twin of the configuration path. */
    private void builderPipeline(IngestBuilderPipelines.Entry entry) {
        String name = entry.name();
        // configuration can still switch a builder-declared pipeline off, and the check precedes
        // the invocation so a disabled pipeline's method never runs
        IngestRunTimeConfig.PipelineRunTimeConfig external = runTimeConfig.pipelines().get(name);
        if (external != null && !external.enabled()) {
            LOG.infof("Ingestion pipeline '%s' (builder) is disabled", name);
            return;
        }
        // enabled is the one thing configuration may say about a builder pipeline; anything about
        // its source would be quietly overruled by the @Ingest method, so it is an error instead
        // (source.recursive cannot be told apart from its default, so it alone goes undetected -
        // Source.recursive() is its builder twin)
        if (external != null && (external.source().directory().isPresent()
                || external.source().documentId().isPresent()
                || external.source().idempotentRepository().isPresent()
                || external.source().idempotentRepositoryAutoCreate())) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' is declared in Java, so its source "
                    + "comes from the @Ingest method. Remove quarkus.camel.langchain4j.ingest." + name + ".source.* , or "
                    + "declare the pipeline in configuration instead.");
        }

        IngestPipeline definition = builderPipelines.definition(entry);
        String uri = "file".equals(definition.sourceType()) ? null : definition.sourceUri();
        compositionRoute(name, uri, definition.asRunTimeConfig(),
                resolveStore(name, definition.embeddingStoreName().orElse(null)),
                resolveModel(name, definition.embeddingModelName().orElse(null)),
                definition.maxSegmentSize(),
                definition.maxOverlapSize(),
                definition.embeddingBatchSize(),
                definition.maxDocumentSize(),
                definition.documentSplitterName().orElse(null),
                definition.parser().orElse(null));
    }

    /**
     * One creation path for both declaration styles, mirroring how a builder pipeline provides
     * the same runtime-configuration view the configuration path reads.
     */
    private void compositionRoute(String name, String uri,
            IngestRunTimeConfig.PipelineRunTimeConfig runtime,
            EmbeddingStore<TextSegment> store, EmbeddingModel model,
            int maxSegmentSize, int maxOverlapSize, int embeddingBatchSize, int maxDocumentSize,
            String documentSplitterName, String parser) {

        // the name is substituted into Kamelet URIs and registry references, so both declaration
        // styles are held to one charset; @Ingest names are already checked at build time
        if (!name.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalStateException(
                    "Ingestion pipeline name '" + name + "' may only contain letters, digits, '.', '_' and '-'");
        }
        String storeRef = bindInstance(name, "store", store);
        String modelRef = bindInstance(name, "model", model);
        String documentId = runtime == null ? null : runtime.source().documentId().orElse(null);
        String repositoryRef = repositoryRef(name, runtime, uri == null);

        // options at their defaults are passed all the same, here and in the other steps: an
        // application-wide camel.kamelet.<kamelet>.* property would otherwise fill them in too
        Map<String, Object> sink = new LinkedHashMap<>();
        sink.put("pipelineName", name);
        sink.put("documentIdHeader", LangChain4jIngestHeaders.DOCUMENT_ID);
        sink.put("maxSegmentSize", String.valueOf(maxSegmentSize));
        sink.put("maxOverlapSize", String.valueOf(maxOverlapSize));
        sink.put("embeddingBatchSize", String.valueOf(embeddingBatchSize));
        // 0, no limit, is the endpoint's default too
        sink.put("maxDocumentSize", String.valueOf(maxDocumentSize));
        sink.put("minDocumentSize", "0");
        if (documentSplitterName != null) {
            sink.put("documentSplitter", "#bean:" + documentSplitterName);
        }
        sink.put("embeddingStore", "#bean:" + storeRef);
        sink.put("embeddingModel", "#bean:" + modelRef);

        if (uri == null) {
            String directory = required(name, runtime == null ? null : runtime.source().directory().orElse(null),
                    "source.directory");
            // substituted into the file-source Kamelet's endpoint URI: a '?' or '#' could
            // inject consumer options - delete=true would consume the user's documents
            if (directory.contains("?") || directory.contains("#")) {
                throw new IllegalStateException("Ingestion pipeline '" + name
                        + "': the directory must not contain '?' or '#' (got '" + directory + "')");
            }
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("directory", directory);
            source.put("recursive", String.valueOf(runtime.source().recursive()));
            // the file consumer's own default poll delay
            source.put("delay", "500");
            if (parser == null) {
                // text is read as UTF-8; a parser receives the raw bytes instead - the format is
                // its business, and a charset conversion would corrupt a binary document
                source.put("charset", "UTF-8");
            }
            source.put("idempotentRepository", "#bean:" + repositoryRef);

            ProcessorDefinition<?> route = from(kameletUri(name, FILE_SOURCE_KAMELET, "source", source))
                    .routeId("langchain4j-ingest-" + name);
            if (documentId != null) {
                // override the source's file-name default; captured before any further step
                route = route.setHeader(LangChain4jIngestHeaders.DOCUMENT_ID, documentIdExpression(documentId));
            }
            // no duplicate pre-check: the source register already filtered duplicates out
            route = parseSteps(route, name, parser, maxDocumentSize, true, null);
            // the file consumer discards the reply and the source register already keeps the
            // same file version from being ingested twice, so no repository goes to the sink.
            // The discarded reply would hide an empty outcome, so it is logged: warned with a
            // parser, since a parse to nothing typically means a missing Tika parser module or an
            // image-only document, and the file's register key is committed
            route.to(kameletUri(name, SINK_KAMELET, "sink", sink)).process(exchange -> {
                IngestResult result = exchange.getMessage().getBody(IngestResult.class);
                if (result == null || result.outcome() != IngestResult.Outcome.EMPTY) {
                    return;
                }
                if (parser != null) {
                    LOG.warnf("Ingestion pipeline '%s': document '%s' parsed to no text and was skipped; its key is"
                            + " committed, so it is not retried until the file changes (missing parser module?"
                            + " image-only document?)", name, result.documentId());
                } else {
                    LOG.debugf("Ingestion pipeline '%s': document '%s' contained no text, nothing was written",
                            name, result.documentId());
                }
            });
            LOG.infof("Ingestion pipeline '%s': source=file:%s", name, directory);
        } else {
            if (!uri.contains(":")) {
                throw new IllegalStateException(
                        "Ingestion pipeline '" + name + "': '" + uri + "' is not a consumer URI");
            }
            ProcessorDefinition<?> route = from(uri).routeId("langchain4j-ingest-" + name);
            if (documentId != null && !IngestHeaders.DOCUMENT_ID.equals(documentId)) {
                // copied into the canonical header the actions read, as on a directory pipeline:
                // evaluated against the exchange the consumer delivered, before any parse, and a
                // plain header name is read directly, never substituted into an expression
                route = route.setHeader(LangChain4jIngestHeaders.DOCUMENT_ID, documentIdExpression(documentId));
                if (!isSimpleExpression(documentId)) {
                    // the sink's missing-id error names the header the route reads, as before; the
                    // parser steps keep the canonical one
                    sink.put("documentIdHeader", documentId);
                }
            } else {
                // unset, or the component's canonical header (the 3.40 rename, #9162): read with the
                // previous fallbacks, its exchange property and the 3.39 name
                route = route.process(documentIdFallback(name));
            }
            // captured into the exchange property before any parse, as by the previous builder: the
            // sink reads the property first, so the id resolved here wins over any header
            route = route.process(exchange -> exchange.setProperty(LangChain4jIngest.DOCUMENT_ID_PROPERTY,
                    exchange.getMessage().getHeader(LangChain4jIngestHeaders.DOCUMENT_ID)));
            IdempotentRepository register = repositoryRef == null
                    ? null
                    : getContext().getRegistry().lookupByNameAndType(repositoryRef, IdempotentRepository.class);
            route = parseSteps(route, name, parser, maxDocumentSize, false, register);
            if (repositoryRef != null) {
                // deduplication by document id happens inside the sink's producer: a duplicate
                // is answered SKIPPED, a blank delivery releases its claim
                sink.put("idempotentRepository", "#bean:" + repositoryRef);
            }
            route.to(kameletUri(name, SINK_KAMELET, "sink", sink));
            LOG.infof("Ingestion pipeline '%s': source=%s", name, URISupport.sanitizeUri(uri));
        }
    }

    /**
     * The optional parse stage: the id guard, the advisory duplicate check and the raw-size
     * guard, then the parser action Kamelet, which captures the document id from the canonical
     * header into the exchange property before the parse. The route is returned unchanged when
     * the pipeline has no parser.
     */
    private static ProcessorDefinition<?> parseSteps(ProcessorDefinition<?> route, String name, String parser,
            int maxDocumentSize, boolean directory, IdempotentRepository register) {
        if (parser == null) {
            return route;
        }
        String documentIdHeader = LangChain4jIngestHeaders.DOCUMENT_ID;
        // a parser must never run without a captured id: the header the action reads would be
        // absent, and headers written by a parsed document could take its place - the previous
        // engine failed such a delivery, and so does this guard
        route = route.process(exchange -> {
            String id = exchange.getMessage().getHeader(documentIdHeader, String.class);
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Ingestion pipeline '" + name
                        + "': the document id resolved to nothing, and a parser pipeline requires it before"
                        + " the parse - a parsed document must not supply its own identity");
            }
        });
        if (register != null) {
            // advisory: a known duplicate is answered SKIPPED before the (possibly remote) parse
            // is paid for; the sink producer's eager claim stays authoritative, so a duplicate
            // racing this check is still caught there
            route = route.choice()
                    .when(exchange -> register.contains(exchange.getMessage().getHeader(documentIdHeader, String.class)))
                    .process(exchange -> exchange.getMessage().setBody(new IngestResult(name,
                            exchange.getMessage().getHeader(documentIdHeader, String.class), 0,
                            IngestResult.Outcome.SKIPPED)))
                    .stop()
                    .end();
        }
        if (maxDocumentSize > 0) {
            // the endpoint's own cap counts extracted characters, which protects the splitter and
            // the model but not the parse: this guard rejects the raw payload first, before tika
            // or docling materialize it
            route = route.process(rawSizeGuard(name, maxDocumentSize, directory, documentIdHeader));
        }
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("documentIdHeader", documentIdHeader);
        return route.to(kameletUri(name, Parser.valueOf(parser.toUpperCase(Locale.ROOT)).actionKamelet, "parser", action));
    }

    private static Processor rawSizeGuard(String name, int maxDocumentSize, boolean trustDeclaredLength,
            String documentIdHeader) {
        // the declared-length header spares even the read, but only the directory pipeline's own
        // file consumer is trusted to have set it: on a consumer pipeline every header may be
        // attacker-supplied along with the payload, so its body is always measured - a forged
        // CamelFileLength must not talk an oversized payload past the guard and into the parser
        return exchange -> {
            Long declared = trustDeclaredLength
                    ? exchange.getMessage().getHeader(Exchange.FILE_LENGTH, Long.class)
                    : null;
            long size;
            byte[] bounded = null;
            if (declared != null) {
                size = declared;
            } else {
                // every body is measured through a stream - a GenericFile streams from disk, a
                // byte[] merely wraps - so an attacker-sized payload is rejected after
                // maxDocumentSize + 1 bytes instead of being materialized whole in the heap just
                // to be measured; an accepted stream is consumed here, so the bytes replace it
                // as the body. A null body carries no bytes to guard; it flows on and becomes
                // the EMPTY outcome
                InputStream stream = exchange.getMessage().getBody(InputStream.class);
                if (stream == null) {
                    size = 0;
                } else {
                    int limit = maxDocumentSize == Integer.MAX_VALUE ? Integer.MAX_VALUE : maxDocumentSize + 1;
                    bounded = stream.readNBytes(limit);
                    size = bounded.length;
                }
            }
            if (size > maxDocumentSize) {
                throw new IllegalArgumentException(
                        "Ingestion pipeline '" + name + "': document '"
                                + exchange.getMessage().getHeader(documentIdHeader, String.class)
                                + "' exceeds maxDocumentSize (" + size + " > " + maxDocumentSize + " bytes)");
            }
            if (bounded != null) {
                exchange.getMessage().setBody(bounded);
            }
        };
    }

    /**
     * Built through {@code createQueryString} rather than concatenated, so no Kamelet property can be injected.
     * Every value travels as a {@code RAW(...)} token: {@code createQueryString} leaves those unencoded and the
     * Kamelet's own URI parsing unwraps them, so a {@code #bean:} prefix, an Ant pattern's slashes and commas, or
     * a directory path arrive at the template verbatim — percent-encoded they would survive the substitution
     * literally. The route the Kamelet adds is named {@code langchain4j-ingest-<name>-<step>} rather than generated.
     * The endpoint's own {@code location} loads the catalog Kamelet from the classpath whatever
     * {@code camel.component.kamelet.location} says; it is appended as is, since the component reads it neither
     * decoded nor unwrapped from a RAW token.
     */
    private static String kameletUri(String name, String kamelet, String step, Map<String, Object> properties) {
        Map<String, Object> raw = new LinkedHashMap<>();
        properties.forEach((key, value) -> raw.put(key, raw(name, String.valueOf(value))));
        return "kamelet:" + kamelet + "/langchain4j-ingest-" + name + "-" + step + "?"
                + URISupport.createQueryString(raw) + "&location=classpath:kamelets";
    }

    private static String raw(String name, String value) {
        if (!value.contains(")")) {
            return "RAW(" + value + ")";
        }
        if (!value.contains("}")) {
            return "RAW{" + value + "}";
        }
        throw new IllegalStateException("Ingestion pipeline '" + name
                + "': a Kamelet property value containing both ')' and '}' cannot be passed: " + value);
    }

    private static boolean isSimpleExpression(String value) {
        return value.contains("${") || value.contains("$simple{");
    }

    /**
     * Resolves the pipeline's duplicate register to a registry reference: a named bean (existence
     * checked up front, with the configuration-level message), an auto-created in-memory register
     * bound under the configured name, or - for a directory pipeline naming none - a generated
     * built-in one, sized above the file endpoint's default so eviction does not re-ingest large
     * directories.
     */
    private String repositoryRef(String name, IngestRunTimeConfig.PipelineRunTimeConfig runtime, boolean directory) {
        String repositoryName = runtime == null ? null : runtime.source().idempotentRepository().orElse(null);
        boolean autoCreate = runtime != null && runtime.source().idempotentRepositoryAutoCreate();
        if (autoCreate && repositoryName == null) {
            throw new IllegalStateException("Ingestion pipeline '" + name
                    + "' sets source.idempotent-repository-auto-create but no "
                    + "source.idempotent-repository name to create the register under.");
        }
        if (repositoryName != null) {
            IdempotentRepository repository = getContext().getRegistry().lookupByNameAndType(repositoryName,
                    IdempotentRepository.class);
            if (repository == null) {
                if (!autoCreate) {
                    throw new IllegalStateException("Ingestion pipeline '" + name
                            + "' references idempotent repository '" + repositoryName + "' but no such bean exists");
                }
                getContext().getRegistry().bind(repositoryName,
                        MemoryIdempotentRepository.memoryIdempotentRepository(DEFAULT_REGISTER_CAPACITY));
            } else {
                // a CDI-produced repository does not pass through the registry's bind hook, so a
                // CamelContextAware implementation would otherwise run contextless
                CamelContextAware.trySetCamelContext(repository, getContext());
            }
            return repositoryName;
        }
        if (directory) {
            String ref = "langchain4j-ingest-" + name + "-register";
            if (getContext().getRegistry().lookupByNameAndType(ref, IdempotentRepository.class) == null) {
                getContext().getRegistry().bind(ref,
                        MemoryIdempotentRepository.memoryIdempotentRepository(DEFAULT_REGISTER_CAPACITY));
            }
            return ref;
        }
        return null;
    }

    /**
     * When the id lives in the default {@code CamelLangChain4jIngestDocumentId} header, it is looked
     * up as the previous builder did: the header, then the exchange property of that name, then the
     * 3.39 name (header, then property), warned once per pipeline. The id found goes into the header.
     */
    @SuppressWarnings("deprecation")
    private static Processor documentIdFallback(String name) {
        AtomicBoolean warned = new AtomicBoolean();
        return exchange -> {
            if (exchange.getMessage().getHeader(IngestHeaders.DOCUMENT_ID) != null) {
                return;
            }
            Object id = exchange.getProperty(IngestHeaders.DOCUMENT_ID);
            if (id == null) {
                id = exchange.getMessage().getHeader(IngestHeaders.LEGACY_DOCUMENT_ID);
                if (id == null) {
                    id = exchange.getProperty(IngestHeaders.LEGACY_DOCUMENT_ID);
                }
                if (id != null && warned.compareAndSet(false, true)) {
                    LOG.warnf("Ingestion pipeline '%s': document id read via the deprecated %s name"
                            + " - switch the producer to %s",
                            name, IngestHeaders.LEGACY_DOCUMENT_ID, IngestHeaders.DOCUMENT_ID);
                }
            }
            if (id != null) {
                exchange.getMessage().setHeader(IngestHeaders.DOCUMENT_ID, id);
            }
        };
    }

    /** Binds a CDI-resolved instance to the registry, so the Kamelet can reference it. */
    private String bindInstance(String name, String what, Object instance) {
        String ref = "langchain4j-ingest-" + name + "-" + what;
        getContext().getRegistry().bind(ref, instance);
        return ref;
    }

    /**
     * A bare header name is read as a header directly rather than parsed: a dotted header name
     * would send the simple parser into OGNL. The expression is initialised here, at route build
     * time — left to reify lazily it would race on the first concurrent exchanges.
     */
    private Expression documentIdExpression(String configured) {
        Expression expression = isSimpleExpression(configured)
                ? ExpressionBuilder.simpleExpression(configured)
                : ExpressionBuilder.headerExpression(configured);
        expression.init(getContext());
        return expression;
    }

    private static String required(String name, String value, String property) {
        if (value == null) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' has no " + property
                    + ". Set quarkus.camel.langchain4j.ingest." + name + "." + property);
        }
        return value;
    }

    private EmbeddingStore<TextSegment> resolveStore(String name, String configured) {
        return resolve(name, storeCandidates, configured, "embedding store", "embedding-store", "embeddingStore");
    }

    private EmbeddingModel resolveModel(String name, String configured) {
        return resolve(name, modelCandidates, configured, "embedding model", "embedding-model", "embeddingModel");
    }

    /**
     * CDI is the one mechanism for both lookups: the named path selects on the qualifier, the
     * unnamed path counts the candidates — through handles, so beans are not instantiated merely
     * to be counted. Picking one silently would bind a pipeline to whichever bean happened to be
     * discovered first. A raw-typed registry search cannot serve here: it never matches a bean
     * typed {@code EmbeddingStore<TextSegment>}.
     */
    private <T> T resolve(String name, Instance<T> candidates, String configured, String what, String property,
            String setter) {
        if (configured != null) {
            Instance<T> named = candidates.select(NamedLiteral.of(configured));
            if (named.isUnsatisfied()) {
                throw new IllegalStateException("Ingestion pipeline '" + name + "' references " + what + " '"
                        + configured + "' but no such bean exists");
            }
            return named.get();
        }
        List<Instance.Handle<T>> handles = StreamSupport.stream(candidates.handles().spliterator(), false)
                .collect(Collectors.toList());
        if (handles.isEmpty()) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' needs an " + what
                    + ", but no bean of that type exists. Define one, for example with a @Produces method.");
        }
        if (handles.size() > 1) {
            // the build rejects configuration keys for the name of a pipeline declared in Java
            boolean java = builderPipelines.entries().stream().anyMatch(entry -> entry.name().equals(name));
            throw new IllegalStateException("Ingestion pipeline '" + name + "' found " + handles.size() + " "
                    + what + " beans. Name the one to use with " + (java
                            ? "IngestPipeline." + setter + "(...) in its @Ingest method"
                            : "quarkus.camel.langchain4j.ingest." + name + "." + property));
        }
        return handles.get(0).get();
    }

    /** The supported document parsers, each with the action Kamelet performing the parse. */
    enum Parser {
        TIKA("tika-extract-text-action"),
        DOCLING("docling-convert-action");

        private final String actionKamelet;

        Parser(String actionKamelet) {
            this.actionKamelet = actionKamelet;
        }
    }
}
