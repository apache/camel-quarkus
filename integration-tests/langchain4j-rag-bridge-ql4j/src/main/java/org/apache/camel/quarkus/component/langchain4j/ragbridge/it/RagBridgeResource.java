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
package org.apache.camel.quarkus.component.langchain4j.ragbridge.it;

import java.util.List;
import java.util.Map;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

@Path("/rag-bridge")
public class RagBridgeResource {

    @Inject
    Instance<RetrievalAugmentor> retrievalAugmentorInstance;

    @Inject
    @Named("products")
    Instance<RetrievalAugmentor> namedAugmentorInstance;

    // the augmentor that is NOT the designated default, so it carries @RagAugmentorName on top
    // of @Named - the qualifier that must keep it selectable by name
    @Inject
    @Named("support")
    Instance<RetrievalAugmentor> supportAugmentorInstance;

    @Inject
    @Named("defaultStore")
    EmbeddingStore<TextSegment> defaultStore;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext camelContext;

    @Inject
    RagAiService aiService;

    @GET
    @Path("/augmentor-present")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isAugmentorPresent() {
        return retrievalAugmentorInstance.isResolvable();
    }

    @GET
    @Path("/named-augmentor-present")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isNamedAugmentorPresent() {
        return namedAugmentorInstance.isResolvable();
    }

    @POST
    @Path("/ingest")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ingest(String text) {
        producerTemplate.sendBody("direct:ingest", text);
        return "ingested";
    }

    @POST
    @Path("/ingest-products")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ingestProducts(String text) {
        producerTemplate.sendBody("direct:ingest-products", text);
        return "ingested";
    }

    @GET
    @Path("/registry/embedding-store/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isEmbeddingStoreInRegistry(@PathParam("name") String name) {
        EmbeddingStore<?> store = camelContext.getRegistry().lookupByNameAndType(name, EmbeddingStore.class);
        return store != null;
    }

    @POST
    @Path("/ask")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ask(String question) {
        return aiService.chat(question);
    }

    @GET
    @Path("/support-augmentor-present")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isSupportAugmentorPresent() {
        return supportAugmentorInstance.isResolvable();
    }

    // --- retrieval filter hook -----------------------------------------------------------

    /** Seeds the default store with a tenant-tagged segment, like the ingest extension does. */
    @POST
    @Path("/seed/{tenant}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String seed(@PathParam("tenant") String tenant, String text) {
        TextSegment segment = TextSegment.from(text, Metadata.from(Map.of("cq_tenant", tenant)));
        defaultStore.add(embeddingModel.embed(segment).content(), segment);
        return "seeded";
    }

    /** Lets a test start from a known store, since the default store is shared with other tests. */
    @DELETE
    @Path("/seed")
    @Produces(MediaType.TEXT_PLAIN)
    public String clearSeeds() {
        defaultStore.removeAll();
        return "cleared";
    }

    @POST
    @Path("/tenant-filter/{tenant}")
    @Produces(MediaType.TEXT_PLAIN)
    public String setTenantFilter(@PathParam("tenant") String tenant) {
        TestTenantFilterSupplier.tenant = "none".equals(tenant) ? null : tenant;
        return "ok";
    }

    @GET
    @Path("/filter/last-augmentor")
    @Produces(MediaType.TEXT_PLAIN)
    public String lastFilteredAugmentor() {
        return String.valueOf(TestTenantFilterSupplier.lastAugmentorName);
    }

    @GET
    @Path("/filter/last-store")
    @Produces(MediaType.TEXT_PLAIN)
    public String lastFilteredStore() {
        return String.valueOf(TestTenantFilterSupplier.lastEmbeddingStoreName);
    }

    /**
     * Runs a produced augmentor directly, returning the retrieved segment texts. {@code memoryId}
     * travels in the query metadata, the only tenant source that survives retrieval running off
     * the caller's thread.
     */
    @POST
    @Path("/augment")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> augment(@QueryParam("augmentor") String augmentor,
            @QueryParam("memoryId") String memoryId, String question) {
        UserMessage userMessage = UserMessage.from(question);
        AugmentationResult result = augmentorFor(augmentor)
                .augment(new AugmentationRequest(userMessage,
                        dev.langchain4j.rag.query.Metadata.from(userMessage, memoryId, null)));
        return result.contents().stream().map(content -> content.textSegment().text()).toList();
    }

    private RetrievalAugmentor augmentorFor(String augmentor) {
        return switch (augmentor == null ? "default" : augmentor) {
        case "products" -> namedAugmentorInstance.get();
        case "support" -> supportAugmentorInstance.get();
        default -> retrievalAugmentorInstance.get();
        };
    }

    @DELETE
    @Path("/filter/last-call")
    @Produces(MediaType.TEXT_PLAIN)
    public String forgetLastFilterCall() {
        TestTenantFilterSupplier.forgetLastCall();
        return "forgotten";
    }

}
