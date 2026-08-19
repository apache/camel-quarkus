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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestHeaders;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestResult;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestService;
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
    @Named("datasheets-store")
    EmbeddingStore<TextSegment> datasheetsStore;

    @Inject
    @Named("s3-store")
    EmbeddingStore<TextSegment> s3Store;

    @Inject
    @Named("events-store")
    EmbeddingStore<TextSegment> eventsStore;

    @Inject
    @Named("reports-store")
    EmbeddingStore<TextSegment> reportsStore;

    @Inject
    @Named("scans-store")
    EmbeddingStore<TextSegment> scansStore;

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
        Path dir = Path.of("scans".equals(pipeline) ? scansDirectory : reportsDirectory);
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
        case "datasheets" -> datasheetsStore;
        case "s3" -> s3Store;
        case "events" -> eventsStore;
        case "reports" -> reportsStore;
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
                        match.embedded().metadata().getString(IngestService.METADATA_PIPELINE),
                        match.embedded().metadata().getString(IngestService.METADATA_DOCUMENT_ID)))
                .toList();
    }

    /** One stored segment with the metadata the pipeline stamped on it. */
    public record SearchHit(String text, String pipeline, String documentId) {
    }

    /** Feeds a pipeline synchronously; the reply carries the outcome, so tests can assert skipped and failures. */
    @POST
    @jakarta.ws.rs.Path("/feed/{pipeline}/{documentId:.+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String feed(@PathParam("pipeline") String pipeline, @PathParam("documentId") String documentId,
            String content) {
        // every consumer-fed test pipeline reads direct:<pipeline>-feed
        String uri = "direct:" + pipeline + "-feed";
        IngestResult result = producerTemplate.requestBodyAndHeader(uri, content, IngestHeaders.DOCUMENT_ID,
                documentId, IngestResult.class);
        return result.outcome().label();
    }

}
