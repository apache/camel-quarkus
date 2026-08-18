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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestHeaders;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestResult;
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
    @Named("built-store")
    EmbeddingStore<TextSegment> builtStore;

    @Inject
    @Named("s3-store")
    EmbeddingStore<TextSegment> s3Store;

    @Inject
    @Named("events-store")
    EmbeddingStore<TextSegment> eventsStore;

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "ingest.test.directory")
    String directory;

    /** Writes a document into the watched directory — app-side, so native mode shares the path. */
    @POST
    @jakarta.ws.rs.Path("/file/{name}")
    @Consumes(MediaType.TEXT_PLAIN)
    public void writeFile(@PathParam("name") String name, String content) throws Exception {
        Path dir = Path.of(directory);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name), content);
    }

    @GET
    @jakarta.ws.rs.Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> search(@QueryParam("q") String query, @QueryParam("store") String storeName) {
        EmbeddingStore<TextSegment> store = switch (storeName == null ? "products" : storeName) {
        case "custom" -> customStore;
        case "built" -> builtStore;
        case "s3" -> s3Store;
        case "events" -> eventsStore;
        default -> productsStore;
        };
        var result = store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(model.embed(query).content())
                .maxResults(20)
                .minScore(0.0)
                .build());
        return result.matches().stream().map(match -> match.embedded().text()).toList();
    }

    /** Feeds a push pipeline through its Camel consumer URI. */
    @POST
    @jakarta.ws.rs.Path("/feed/{pipeline}/{documentId:.+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String feed(@PathParam("pipeline") String pipeline, @PathParam("documentId") String documentId,
            String content) {
        String uri = "built".equals(pipeline) ? "direct:built-source" : "direct:custom-source";
        IngestResult result = producerTemplate.requestBodyAndHeader(uri, content, IngestHeaders.DOCUMENT_ID,
                documentId, IngestResult.class);
        return result.outcome().label();
    }

}
