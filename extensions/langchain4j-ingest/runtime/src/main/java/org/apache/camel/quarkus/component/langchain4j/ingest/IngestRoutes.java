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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.endpoint.dsl.FileEndpointBuilderFactory;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestService;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.builder.ExpressionBuilder;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.util.URISupport;
import org.jboss.logging.Logger;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.file;
import static org.apache.camel.quarkus.component.langchain4j.ingest.IngestParsers.parseStep;
import static org.apache.camel.quarkus.component.langchain4j.ingest.IngestSteps.DOCUMENT_ID_PROPERTY;
import static org.apache.camel.quarkus.component.langchain4j.ingest.IngestSteps.answerDuplicateProcessor;
import static org.apache.camel.quarkus.component.langchain4j.ingest.IngestSteps.claimedIngestProcessor;
import static org.apache.camel.quarkus.component.langchain4j.ingest.IngestSteps.directoryIngestProcessor;
import static org.apache.camel.quarkus.component.langchain4j.ingest.IngestSteps.plainIngestProcessor;
import static org.apache.camel.quarkus.component.langchain4j.ingest.IngestSteps.resolveDocumentIdProcessor;

/**
 * Generates one Camel route per ingestion pipeline. Users never see these routes — they are the
 * implementation of the configuration.
 *
 * <p>
 * Three declaration styles — build-time configuration, runtime-only configuration and
 * {@code @Ingest} builder methods — funnel into the one {@link #configurePipeline} creation
 * path, which dispatches to one of two route topologies. A directory pipeline deduplicates in
 * its endpoint: the file consumer's register, keyed on path, modification time and size. A
 * consumer-fed pipeline deduplicates in the route: an idempotentConsumer claim keyed on the
 * resolved document id. The named route steps live in {@link IngestSteps}, the parse stage in
 * {@link IngestParsers}, and bean lookups in {@link IngestBeanResolver}.
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

    @Inject
    IngestBeanResolver beans;

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

            configurePipeline(name, pipeline, runtime, "");
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
        configurePipeline(name, definition.asBuildTimeConfig(), definition.asRunTimeConfig(), " (builder)");
    }

    /**
     * One pipeline-creation path for both declaration styles: a builder pipeline provides the
     * same configuration views the configuration path reads, so everything downstream — service
     * creation, source dispatch, the routes — is shared verbatim. Only the configuration path
     * can lack a build-time entry (a pipeline declared through runtime properties alone), hence
     * the null fallbacks.
     */
    private void configurePipeline(String name, IngestBuildTimeConfig.PipelineBuildTimeConfig pipeline,
            IngestRunTimeConfig.PipelineRunTimeConfig runtime, String origin) {
        IngestService service = new IngestService(
                name,
                beans.resolveStore(name, pipeline == null ? null : pipeline.embeddingStore().orElse(null)),
                beans.resolveModel(name, pipeline == null ? null : pipeline.embeddingModel().orElse(null)),
                pipeline == null ? IngestBuildTimeConfig.DEFAULT_MAX_SEGMENT_SIZE : pipeline.maxSegmentSize(),
                pipeline == null ? IngestBuildTimeConfig.DEFAULT_MAX_OVERLAP_SIZE : pipeline.maxOverlapSize());

        // a consumer URI says "consume from this"; its absence says "read that directory"
        String uri = pipeline == null ? null : pipeline.source().uri().orElse(null);
        if (uri != null && runtime != null && runtime.source().directory().isPresent()) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' sets both source.uri ('" + uri
                    + "') and source.directory ('" + runtime.source().directory().get() + "'). A pipeline "
                    + "reads one source: keep the URI, or drop it to read the directory.");
        }
        PipelineSpec spec = new PipelineSpec(name, runtime, service,
                IngestParsers.Parser.of(pipeline == null ? null : pipeline.parser().orElse(null)));
        if (uri == null) {
            directoryRoute(spec);
            LOG.infof("Ingestion pipeline '%s'%s: source=file", name, origin);
        } else {
            consumerRoute(spec, uri);
            LOG.infof("Ingestion pipeline '%s'%s: source=%s", name, origin, URISupport.sanitizeUri(uri));
        }
    }

    // ********************************************************************************
    // The two route topologies
    // ********************************************************************************

    private void directoryRoute(PipelineSpec spec) {
        String directory = required(spec.name(),
                spec.runtime() == null ? null : spec.runtime().source().directory().orElse(null),
                "source.directory");
        Expression documentId = documentIdExpression(spec.runtime(), Exchange.FILE_NAME);
        beans.maybeAutoCreateRepository(spec.name(), spec.runtime());

        // the route: watch the directory -> resolve the id -> (parse) -> split, embed, store;
        // the register in the endpoint keeps unchanged files from re-ingesting. The id is
        // captured before the parse: a parser copies document metadata over the headers, so a
        // crafted document could otherwise forge its own identity
        ProcessorDefinition<?> route = from(fileEndpoint(spec, directory))
                .routeId(routeId(spec.name()))
                .process(resolveDocumentIdProcessor(spec, documentId));
        route = parseStep(route, spec.parser());
        route.process(directoryIngestProcessor(spec));
    }

    /**
     * The directory consumer. Built with the Endpoint DSL rather than concatenated: a directory
     * containing ? # &amp; or a space would otherwise mis-parse, and a crafted one could inject
     * options - delete=true is honoured ahead of noop and would delete the user's documents
     * after reading them. noop leaves the documents where they are (a knowledge base reads its
     * source, it does not consume it), idempotent keeps the same file from being ingested twice,
     * and the changed read lock waits for a file still being copied in rather than embedding
     * half of it.
     */
    private FileEndpointBuilderFactory.FileEndpointConsumerBuilder fileEndpoint(PipelineSpec spec, String directory) {
        // the runtime view is never null here: a directory pipeline without one would have
        // failed the required(source.directory) check before this method is reached
        String repositoryName = spec.runtime().source().idempotentRepository().orElse(null);
        var endpoint = file(directory)
                .noop(true)
                .idempotent(true);
        if (repositoryName != null) {
            endpoint.idempotentRepository(beans.resolveRepository(spec.name(), repositoryName));
        } else {
            endpoint.idempotentRepository(
                    MemoryIdempotentRepository.memoryIdempotentRepository(IngestBeanResolver.DEFAULT_REGISTER_CAPACITY));
        }
        endpoint
                // an edited file gets a new key and re-ingests; old segments remain (append)
                .idempotentKey("${file:absolute.path}:${file:modified}:${file:size}")
                .recursive(spec.runtime().source().recursive())
                .readLock("changed");
        if (spec.parser() == null) {
            // text is read as UTF-8; a parser receives the raw bytes instead - the format is its
            // business, and a charset conversion would corrupt a binary document
            endpoint.charset(StandardCharsets.UTF_8.name());
        }
        return endpoint;
    }

    /**
     * The escape hatch: any Camel consumer feeds the pipeline. Which part of the exchange
     * identifies the document is the consumer's business, so {@code source.document-id} says it —
     * {@code ${header.CamelAwsS3Key}} for an S3 consumer, the message header otherwise.
     */
    private void consumerRoute(PipelineSpec spec, String uri) {
        Expression documentId = documentIdExpression(spec.runtime(), IngestHeaders.DOCUMENT_ID);
        beans.maybeAutoCreateRepository(spec.name(), spec.runtime());
        String repositoryName = spec.runtime() == null
                ? null : spec.runtime().source().idempotentRepository().orElse(null);

        if (repositoryName == null) {
            // the route: consume -> resolve the id -> (parse) -> split, embed, store; the id
            // is captured before the parse for the same anti-spoofing reason as everywhere
            ProcessorDefinition<?> route = from(uri)
                    .routeId(routeId(spec.name()))
                    .process(resolveDocumentIdProcessor(spec, documentId));
            route = parseStep(route, spec.parser());
            route.process(plainIngestProcessor(spec));
            return;
        }

        // the route: consume -> resolve the id -> claim it in the register [ -> (parse) ->
        // split, embed, store ] -> answer duplicates SKIPPED. A duplicate skips the claimed
        // block without paying for a parse; first write wins per id. The EIP keys on the
        // validated id property, evaluating the expression once
        IdempotentRepository repository = beans.resolveRepository(spec.name(), repositoryName);
        ProcessorDefinition<?> route = from(uri)
                .routeId(routeId(spec.name()))
                .process(resolveDocumentIdProcessor(spec, documentId))
                .idempotentConsumer(exchangeProperty(DOCUMENT_ID_PROPERTY), repository);
        route = parseStep(route, spec.parser());
        route.process(claimedIngestProcessor(spec, repository))
                .end()
                .process(answerDuplicateProcessor(spec));
    }

    // ********************************************************************************
    // Helper methods
    // ********************************************************************************

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
}
