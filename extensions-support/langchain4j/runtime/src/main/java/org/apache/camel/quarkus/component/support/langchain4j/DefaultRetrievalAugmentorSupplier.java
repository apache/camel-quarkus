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
package org.apache.camel.quarkus.component.support.langchain4j;

import java.util.function.Supplier;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkiverse.langchain4j.EmbeddingStoreName;
import io.quarkus.arc.Arc;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.util.TypeLiteral;
import org.jboss.logging.Logger;

/**
 * Lazily creates a {@link RetrievalAugmentor} backed by the CDI {@link EmbeddingStore}
 * and {@link EmbeddingModel} beans. Used as a supplier for the synthetic bean produced
 * by the deployment processor.
 */
public class DefaultRetrievalAugmentorSupplier implements Supplier<RetrievalAugmentor> {

    private static final Logger LOG = Logger.getLogger(DefaultRetrievalAugmentorSupplier.class);

    private static final TypeLiteral<EmbeddingStore<TextSegment>> EMBEDDING_STORE_TYPE = new TypeLiteral<>() {
    };

    private final String embeddingStoreName;
    private final String embeddingModelName;

    public DefaultRetrievalAugmentorSupplier() {
        this(null, null);
    }

    public DefaultRetrievalAugmentorSupplier(String embeddingStoreName, String embeddingModelName) {
        this.embeddingStoreName = embeddingStoreName;
        this.embeddingModelName = embeddingModelName;
    }

    @Override
    public RetrievalAugmentor get() {
        EmbeddingStore<TextSegment> store;
        if (embeddingStoreName != null) {
            store = Arc.container()
                    .instance(EMBEDDING_STORE_TYPE, EmbeddingStoreName.Literal.of(embeddingStoreName)).get();
            if (store == null) {
                store = Arc.container()
                        .instance(EMBEDDING_STORE_TYPE, NamedLiteral.of(embeddingStoreName)).get();
            }
            if (store == null) {
                throw new IllegalStateException(
                        "No EmbeddingStore CDI bean found with @EmbeddingStoreName(\"" + embeddingStoreName
                                + "\") or @Named(\"" + embeddingStoreName + "\")");
            }
        } else {
            store = Arc.container().instance(EMBEDDING_STORE_TYPE).get();
            if (store == null) {
                throw new IllegalStateException("No default EmbeddingStore CDI bean found");
            }
        }

        EmbeddingModel model;
        if (embeddingModelName != null) {
            model = Arc.container()
                    .instance(EmbeddingModel.class, NamedLiteral.of(embeddingModelName)).get();
            if (model == null) {
                throw new IllegalStateException(
                        "No EmbeddingModel CDI bean found with @Named(\"" + embeddingModelName + "\")");
            }
        } else {
            model = Arc.container().instance(EmbeddingModel.class).get();
            if (model == null) {
                throw new IllegalStateException("No default EmbeddingModel CDI bean found");
            }
        }

        LOG.debugf("Creating default RetrievalAugmentor bridging Camel ingestion with @RegisterAiService RAG"
                + " (store=%s, model=%s)", embeddingStoreName != null ? embeddingStoreName : "@Default",
                embeddingModelName != null ? embeddingModelName : "@Default");

        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(EmbeddingStoreContentRetriever.builder()
                        .embeddingStore(store)
                        .embeddingModel(model)
                        .build())
                .build();
    }
}
