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
package org.apache.camel.quarkus.component.langchain4j.ingest.it;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.langchain4j.ingest.IngestResult;
import org.apache.camel.component.langchain4j.ingest.LangChain4jIngest;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestHeaders;
import org.apache.camel.spi.IdempotentRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@jakarta.ws.rs.Path("/langchain4j-ingest")
public class IngestResource {

    @Inject
    @Named("products-store")
    EmbeddingStore<TextSegment> productsStore;

    @Inject
    @Named("test-model")
    EmbeddingModel model;

    @Inject
    @Named("custom-store")
    EmbeddingStore<TextSegment> customStore;

    @Inject
    @Named("htmlfeed-store")
    EmbeddingStore<TextSegment> htmlfeedStore;

    @Inject
    @Named("dottedfeed-store")
    EmbeddingStore<TextSegment> dottedfeedStore;

    @Inject
    @Named("datasheets-store")
    EmbeddingStore<TextSegment> datasheetsStore;

    @Inject
    @Named("s3-store")
    EmbeddingStore<TextSegment> s3Store;

    @Inject
    @Named("events-store")
    EmbeddingStore<TextSegment> eventsStore;

    @Inject
    @Named("jdbc-store")
    EmbeddingStore<TextSegment> jdbcStore;

    @Inject
    @Named("reports-store")
    EmbeddingStore<TextSegment> reportsStore;

    @Inject
    @Named("scans-store")
    EmbeddingStore<TextSegment> scansStore;

    @Inject
    @Named("capped-store")
    EmbeddingStore<TextSegment> cappedStore;

    @Inject
    @Named("filtered-store")
    EmbeddingStore<TextSegment> filteredStore;

    @Inject
    @Named("audio-store")
    EmbeddingStore<TextSegment> audioStore;

    @Inject
    @Named("audio-model")
    DeterministicAudioEmbeddingModel audioModel;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext camelContext;

    @ConfigProperty(name = "ingest.test.directory")
    String directory;

    @ConfigProperty(name = "ingest.reports.directory")
    String reportsDirectory;

    @ConfigProperty(name = "ingest.scans.directory")
    String scansDirectory;

    @ConfigProperty(name = "ingest.capped.directory")
    String cappedDirectory;

    @ConfigProperty(name = "ingest.audio.directory")
    String audioDirectory;

    /** Asserts a key was committed; registry lookup by name, the same way the pipelines resolve. */
    @GET
    @jakarta.ws.rs.Path("/register-contains")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean registerContains(@QueryParam("repo") String repo, @QueryParam("key") String key) {
        IdempotentRepository repository = camelContext.getRegistry().lookupByNameAndType(repo,
                IdempotentRepository.class);
        return repository != null && repository.contains(key);
    }

    /** Writes a document into the watched directory — app-side, so native mode shares the path. */
    @POST
    @jakarta.ws.rs.Path("/file/{name}")
    @Consumes(MediaType.TEXT_PLAIN)
    public void writeFile(@PathParam("name") String name, String content) throws Exception {
        Path dir = Path.of(directory);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name), content);
    }

    /** Writes a binary document into a parser pipeline's watched directory. */
    @POST
    @jakarta.ws.rs.Path("/binary/{pipeline}/{name}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void writeBinary(@PathParam("pipeline") String pipeline, @PathParam("name") String name,
            byte[] content) throws Exception {
        Path dir = Path.of(switch (pipeline) {
        case "scans" -> scansDirectory;
        case "capped" -> cappedDirectory;
        case "audio" -> audioDirectory;
        default -> reportsDirectory;
        });
        Files.createDirectories(dir);
        Files.write(dir.resolve(name), content);
    }

    @GET
    @jakarta.ws.rs.Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SearchHit> search(@QueryParam("q") String query, @QueryParam("store") String storeName) {
        EmbeddingStore<TextSegment> store = switch (storeName == null ? "products" : storeName) {
        case "custom" -> customStore;
        case "htmlfeed" -> htmlfeedStore;
        case "dottedfeed" -> dottedfeedStore;
        case "datasheets" -> datasheetsStore;
        case "s3" -> s3Store;
        case "events" -> eventsStore;
        case "jdbc" -> jdbcStore;
        case "reports" -> reportsStore;
        case "capped" -> cappedStore;
        case "filtered" -> filteredStore;
        case "scans" -> scansStore;
        default -> productsStore;
        };
        // the deterministic test model gives a query no semantic pull towards any document, so
        // with minScore 0 this returns the whole store: the tests assert what was ingested, not
        // how it ranks. Sized far above anything the tests write so nothing is silently dropped
        var result = store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(model.embed(query).content())
                .maxResults(1000)
                .minScore(0.0)
                .build());
        return result.matches().stream()
                .map(match -> new SearchHit(
                        match.embedded().text(),
                        match.embedded().metadata().getString(LangChain4jIngest.METADATA_PIPELINE),
                        match.embedded().metadata().getString(LangChain4jIngest.METADATA_DOCUMENT_ID)))
                .toList();
    }

    /** Query by audio: the clip is embedded with the audio model and the audio store searched for exact hits. */
    @POST
    @jakarta.ws.rs.Path("/search/audio")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public List<SearchHit> searchAudio(byte[] clip) {
        var result = audioStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(audioModel.embeddingOf(clip))
                .maxResults(10)
                .minScore(0.99)
                .build());
        return result.matches().stream()
                .map(match -> new SearchHit(
                        match.embedded().text(),
                        match.embedded().metadata().getString(LangChain4jIngest.METADATA_PIPELINE),
                        match.embedded().metadata().getString(LangChain4jIngest.METADATA_DOCUMENT_ID)))
                .toList();
    }

    /** One stored segment with the metadata the pipeline stamped on it. */
    public record SearchHit(String text, String pipeline, String documentId) {
    }

    /**
     * Feeds a pipeline synchronously; the reply carries the outcome, so tests can assert skipped and failures.
     * The id travels in the {@code header} query parameter's header, the canonical one by default.
     */
    @POST
    @jakarta.ws.rs.Path("/feed/{pipeline}/{documentId:.+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String feed(@PathParam("pipeline") String pipeline, @PathParam("documentId") String documentId,
            @QueryParam("header") String header, String content) {
        // every consumer-fed test pipeline reads direct:<pipeline>-feed
        String uri = "direct:" + pipeline + "-feed";
        IngestResult result = producerTemplate.requestBodyAndHeader(uri, content,
                header == null ? IngestHeaders.DOCUMENT_ID : header, documentId, IngestResult.class);
        return result.outcome().label();
    }

    /** Feeds a media pipeline the raw bytes; the reply carries the outcome. */
    @POST
    @jakarta.ws.rs.Path("/feed-binary/{pipeline}/{documentId:.+}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public String feedBinary(@PathParam("pipeline") String pipeline, @PathParam("documentId") String documentId,
            byte[] content) {
        IngestResult result = producerTemplate.requestBodyAndHeader("direct:" + pipeline + "-feed", content,
                IngestHeaders.DOCUMENT_ID, documentId, IngestResult.class);
        return result.outcome().label();
    }

    /** Feeds a pipeline carrying different ids in the current and the deprecated header, so tests can assert precedence. */
    @POST
    @jakarta.ws.rs.Path("/feed-both/{pipeline}/{documentId}/{legacyDocumentId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("deprecation")
    public String feedBoth(@PathParam("pipeline") String pipeline, @PathParam("documentId") String documentId,
            @PathParam("legacyDocumentId") String legacyDocumentId, String content) {
        IngestResult result = producerTemplate.requestBodyAndHeaders("direct:" + pipeline + "-feed", content,
                Map.of(IngestHeaders.DOCUMENT_ID, documentId, IngestHeaders.LEGACY_DOCUMENT_ID, legacyDocumentId),
                IngestResult.class);
        return result.outcome().label();
    }

    /** Feeds a pipeline carrying the id in the deprecated 3.39 header, so tests can assert the fallback. */
    @POST
    @jakarta.ws.rs.Path("/feed-legacy/{pipeline}/{documentId:.+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("deprecation")
    public String feedLegacy(@PathParam("pipeline") String pipeline, @PathParam("documentId") String documentId,
            String content) {
        IngestResult result = producerTemplate.requestBodyAndHeader("direct:" + pipeline + "-feed", content,
                IngestHeaders.LEGACY_DOCUMENT_ID, documentId, IngestResult.class);
        return result.outcome().label();
    }

    /** Feeds a pipeline carrying the id only as the exchange property; answers the outcome or the failure message. */
    @POST
    @jakarta.ws.rs.Path("/feed-property/{pipeline}/{documentId:.+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String feedProperty(@PathParam("pipeline") String pipeline, @PathParam("documentId") String documentId,
            String content) {
        Exchange exchange = producerTemplate.request("direct:" + pipeline + "-feed", e -> {
            e.setProperty(LangChain4jIngest.DOCUMENT_ID_PROPERTY, documentId);
            e.getMessage().setBody(content);
        });
        return exchange.getException() != null
                ? exchange.getException().getMessage()
                : exchange.getMessage().getBody(IngestResult.class).outcome().label();
    }

    /** Feeds a pipeline without any document id, so tests can assert the pre-parse id guard. */
    @POST
    @jakarta.ws.rs.Path("/feed-anonymous/{pipeline}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String feedAnonymous(@PathParam("pipeline") String pipeline, String content) {
        try {
            producerTemplate.requestBody("direct:" + pipeline + "-feed", content, IngestResult.class);
            return "ingested";
        } catch (Exception e) {
            return "failed";
        }
    }

}
