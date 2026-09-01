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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestResult;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestService;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.builder.ExpressionBuilder;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.util.URISupport;
import org.jboss.logging.Logger;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.file;

/**
 * Generates one Camel route per configured ingestion pipeline. Users never see these routes —
 * they are the implementation of the configuration.
 */
@ApplicationScoped
public class IngestRoutes extends RouteBuilder {

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

    /** Exchange property carrying the resolved document id. */
    private static final String DOCUMENT_ID_PROPERTY = "CamelQuarkusIngestDocumentId";

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

        for (String name : names) {
            IngestBuildTimeConfig.PipelineBuildTimeConfig pipeline = buildTimeConfig.pipelines().get(name);
            IngestRunTimeConfig.PipelineRunTimeConfig runtime = runTimeConfig.pipelines().get(name);

            if (runtime != null && !runtime.enabled()) {
                LOG.infof("Ingestion pipeline '%s' is disabled", name);
                continue;
            }

            IngestService service = new IngestService(
                    name,
                    resolveStore(name, pipeline == null ? null : pipeline.embeddingStore().orElse(null)),
                    resolveModel(name, pipeline == null ? null : pipeline.embeddingModel().orElse(null)),
                    pipeline == null ? IngestBuildTimeConfig.DEFAULT_MAX_SEGMENT_SIZE : pipeline.maxSegmentSize(),
                    pipeline == null ? IngestBuildTimeConfig.DEFAULT_MAX_OVERLAP_SIZE : pipeline.maxOverlapSize());

            // a consumer URI says "consume from this"; its absence says "read that directory"
            String uri = pipeline == null ? null : pipeline.source().uri().orElse(null);
            if (uri != null && runtime != null && runtime.source().directory().isPresent()) {
                throw new IllegalStateException("Ingestion pipeline '" + name + "' sets both source.uri ('" + uri
                        + "') and source.directory ('" + runtime.source().directory().get() + "'). A pipeline "
                        + "reads one source: keep the URI, or drop it to read the directory.");
            }
            if (uri == null) {
                configureFileSource(name, runtime, service);
                LOG.infof("Ingestion pipeline '%s': source=file", name);
            } else {
                configureEndpointSource(name, uri, runtime, service);
                LOG.infof("Ingestion pipeline '%s': source=%s", name, URISupport.sanitizeUri(uri));
            }
        }

        for (IngestBuilderPipelines.Entry entry : builderPipelines.entries()) {
            configureBuilderPipeline(entry);
        }
    }

    /** An {@code @Ingest}-declared pipeline: the builder twin of the configuration path. */
    private void configureBuilderPipeline(IngestBuilderPipelines.Entry entry) {
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
        IngestRunTimeConfig.PipelineRunTimeConfig runtime = definition.asRunTimeConfig();

        IngestService service = new IngestService(
                name,
                resolveStore(name, definition.embeddingStoreName().orElse(null)),
                resolveModel(name, definition.embeddingModelName().orElse(null)),
                definition.maxSegmentSize(),
                definition.maxOverlapSize());

        switch (definition.sourceType()) {
        case "file" -> configureFileSource(name, runtime, service);
        case "endpoint" -> configureEndpointSource(name, definition.sourceUri(), runtime, service);
        default -> throw new IllegalStateException("Unknown source type " + definition.sourceType());
        }

        LOG.infof("Ingestion pipeline '%s' (builder): source=%s", name,
                URISupport.sanitizeUri(definition.sourceUri() == null ? definition.sourceType() : definition.sourceUri()));
    }

    private void configureFileSource(String name, IngestRunTimeConfig.PipelineRunTimeConfig runtime,
            IngestService service) {
        String directory = required(name, runtime == null ? null : runtime.source().directory().orElse(null),
                "source.directory");
        // built with the Endpoint DSL rather than concatenated: a directory containing ? # & or a
        // space would otherwise mis-parse, and a crafted one could inject options - delete=true
        // is honoured ahead of noop and would delete the user's documents after reading them.
        // noop leaves the documents where they are (a knowledge base reads its source, it does
        // not consume it), idempotent keeps the same file from being ingested twice, and the
        // changed read lock waits for a file still being copied in rather than embedding half
        // of it
        Expression documentId = documentIdExpression(runtime, Exchange.FILE_NAME);
        maybeAutoCreateRepository(name, runtime);
        String repositoryName = runtime == null ? null : runtime.source().idempotentRepository().orElse(null);
        var endpoint = file(directory)
                .noop(true)
                .idempotent(true);
        if (repositoryName != null) {
            endpoint.idempotentRepository(resolveRepository(name, repositoryName));
        } else {
            // in-memory default, sized above Camel's 1000-entry cap so eviction does not
            // re-ingest large directories; lost on restart
            endpoint.idempotentRepository(MemoryIdempotentRepository.memoryIdempotentRepository(100_000));
        }
        from(endpoint
                // an edited file gets a new key and re-ingests; old segments remain (append)
                .idempotentKey("${file:absolute.path}:${file:modified}:${file:size}")
                .recursive(runtime.source().recursive())
                .readLock("changed")
                .charset(StandardCharsets.UTF_8.name()))
                .routeId(routeId(name))
                .process(exchange -> {
                    IngestResult result = service.ingest(documentId.evaluate(exchange, String.class),
                            exchange.getIn().getBody(String.class));
                    // the file consumer discards the result, so an EMPTY outcome would
                    // otherwise leave no trace at all
                    if (result.outcome() == IngestResult.Outcome.EMPTY) {
                        LOG.debugf("Ingestion pipeline '%s': document '%s' contained no text, nothing was written",
                                name, result.documentId());
                    }
                });
    }

    /**
     * The escape hatch: any Camel consumer feeds the pipeline. Which part of the exchange
     * identifies the document is the consumer's business, so {@code source.document-id} says it —
     * {@code ${header.CamelAwsS3Key}} for an S3 consumer, the message header otherwise.
     */
    private void configureEndpointSource(String name, String uri,
            IngestRunTimeConfig.PipelineRunTimeConfig runtime, IngestService service) {
        Expression documentId = documentIdExpression(runtime, IngestHeaders.DOCUMENT_ID);
        maybeAutoCreateRepository(name, runtime);
        String repositoryName = runtime == null ? null : runtime.source().idempotentRepository().orElse(null);
        if (repositoryName == null) {
            from(uri)
                    .routeId(routeId(name))
                    .process(exchange -> {
                        String id = requireDocumentId(name, documentId, exchange);
                        exchange.getIn().setBody(service.ingest(id, exchange.getIn().getBody(String.class)));
                    });
            return;
        }
        // duplicates skip the block and the tail processor answers SKIPPED; first write wins
        // per id. The EIP keys on the validated id property, evaluating the expression once
        IdempotentRepository repository = resolveRepository(name, repositoryName);
        from(uri)
                .routeId(routeId(name))
                .process(exchange -> exchange.setProperty(DOCUMENT_ID_PROPERTY,
                        requireDocumentId(name, documentId, exchange)))
                .idempotentConsumer(exchangeProperty(DOCUMENT_ID_PROPERTY), repository)
                .process(exchange -> {
                    String id = (String) exchange.getProperty(DOCUMENT_ID_PROPERTY);
                    IngestResult result = service.ingest(id, exchange.getIn().getBody(String.class));
                    if (result.outcome() == IngestResult.Outcome.EMPTY) {
                        // a blank document wrote nothing, so it must not keep the eager claim
                        // on the id - a later, populated delivery under the same id would be
                        // answered SKIPPED. The completion-time confirm() of the removed key
                        // is a no-op in the memory, file and JDBC repositories alike
                        repository.remove(id);
                    }
                    exchange.getIn().setBody(result);
                })
                .end()
                .process(exchange -> {
                    if (exchange.getProperty(Exchange.DUPLICATE_MESSAGE, false, Boolean.class)) {
                        exchange.getIn().setBody(new IngestResult(name,
                                (String) exchange.getProperty(DOCUMENT_ID_PROPERTY), 0,
                                IngestResult.Outcome.SKIPPED));
                    }
                });
    }

    private static String requireDocumentId(String name, Expression documentId, Exchange exchange) {
        String id = documentId.evaluate(exchange, String.class);
        if (id == null) {
            throw new IllegalArgumentException("Ingestion pipeline '" + name + "': no document id. "
                    + "Set the " + IngestHeaders.DOCUMENT_ID + " header, or point "
                    + "quarkus.camel.langchain4j.ingest." + name + ".source.document-id at where the "
                    + "consumer puts it.");
        }
        return id;
    }

    private Expression documentIdExpression(IngestRunTimeConfig.PipelineRunTimeConfig runtime,
            String defaultHeader) {
        String configured = runtime == null ? null : runtime.source().documentId().orElse(null);
        if (configured == null) {
            return ExpressionBuilder.headerExpression(defaultHeader);
        }
        // a bare header name is read as a header directly rather than parsed: a dotted header
        // name would send the simple parser into OGNL. $simple{...} is the form for a properties
        // file, where MicroProfile Config would consume a ${...} before Camel ever saw it; the
        // ${...} form still serves the Java builder. Both tokens are matched with contains, the
        // same way Camel's own LanguageSupport detects a simple function. The expression is
        // initialised here, at route build time - left to reify lazily it would race on the
        // first concurrent exchanges
        Expression expression = configured.contains("${") || configured.contains("$simple{")
                ? ExpressionBuilder.simpleExpression(configured)
                : ExpressionBuilder.headerExpression(configured);
        expression.init(getContext());
        return expression;
    }

    private static String routeId(String name) {
        return "camel-quarkus-langchain4j-ingest-" + name;
    }

    private static String required(String name, String value, String property) {
        if (value == null) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' has no " + property
                    + ". Set quarkus.camel.langchain4j.ingest." + name + "." + property);
        }
        return value;
    }

    private EmbeddingStore<TextSegment> resolveStore(String name, String configured) {
        return resolve(name, storeCandidates, configured, "embedding store", "embedding-store");
    }

    private EmbeddingModel resolveModel(String name, String configured) {
        return resolve(name, modelCandidates, configured, "embedding model", "embedding-model");
    }

    /**
     * CDI is the one mechanism for both lookups: the named path selects on the qualifier, the
     * unnamed path counts the candidates — through handles, so beans are not instantiated merely
     * to be counted. Picking one silently would bind a pipeline to whichever bean happened to be
     * discovered first. A raw-typed registry search cannot serve here: it never matches a bean
     * typed {@code EmbeddingStore<TextSegment>}.
     */
    /**
     * Binds an in-memory register under the configured name, unless a bean with that name
     * already exists — {@code camel.beans.*} beans are bound before route builders run, so both
     * they and CDI beans are visible here and win.
     */
    private void maybeAutoCreateRepository(String name, IngestRunTimeConfig.PipelineRunTimeConfig runtime) {
        if (runtime == null || !runtime.source().idempotentRepositoryAutoCreate()) {
            return;
        }
        String repositoryName = runtime.source().idempotentRepository().orElse(null);
        if (repositoryName == null) {
            throw new IllegalStateException("Ingestion pipeline '" + name
                    + "' sets source.idempotent-repository-auto-create but no "
                    + "source.idempotent-repository name to create the register under.");
        }
        if (getContext().getRegistry().lookupByNameAndType(repositoryName, IdempotentRepository.class) != null) {
            LOG.infof("Ingestion pipeline '%s': idempotent repository '%s' already exists, auto-create skipped",
                    name, repositoryName);
            return;
        }
        getContext().getRegistry().bind(repositoryName,
                MemoryIdempotentRepository.memoryIdempotentRepository(100_000));
    }

    /**
     * Resolves the configured register from the Camel registry: CDI producers, camel.beans
     * definitions and auto-created registers alike. By name only — the application may hold
     * unrelated idempotent repositories.
     */
    private IdempotentRepository resolveRepository(String name, String repositoryName) {
        IdempotentRepository repository = getContext().getRegistry().lookupByNameAndType(repositoryName,
                IdempotentRepository.class);
        if (repository == null) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' references idempotent repository '"
                    + repositoryName + "' but no such bean exists");
        }
        // a CDI-produced repository does not pass through the registry's bind hook, so a
        // CamelContextAware implementation would otherwise run contextless
        CamelContextAware.trySetCamelContext(repository, getContext());
        return repository;
    }

    private <T> T resolve(String name, Instance<T> candidates, String configured, String what, String property) {
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
            throw new IllegalStateException("Ingestion pipeline '" + name + "' found " + handles.size() + " "
                    + what + " beans. Name the one to use with quarkus.camel.langchain4j.ingest." + name + "."
                    + property);
        }
        return handles.get(0).get();
    }
}
