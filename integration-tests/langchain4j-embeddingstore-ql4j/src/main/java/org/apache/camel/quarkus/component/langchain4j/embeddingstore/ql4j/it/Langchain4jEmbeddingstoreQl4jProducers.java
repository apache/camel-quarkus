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
package org.apache.camel.quarkus.component.langchain4j.embeddingstore.ql4j.it;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.CamelContext;
import org.apache.camel.component.langchain4j.embeddingstore.EmbeddingStoreFactory;

@ApplicationScoped
public class Langchain4jEmbeddingstoreQl4jProducers {
    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Produces
    @Singleton
    EmbeddingStoreFactory embeddingStoreFactory() {
        return new EmbeddingStoreFactory() {
            CamelContext camelContext;

            @Override
            public EmbeddingStore<TextSegment> createEmbeddingStore() {
                return embeddingStore;
            }

            @Override
            public void setCamelContext(CamelContext camelContext) {
                this.camelContext = camelContext;
            }

            @Override
            public CamelContext getCamelContext() {
                return this.camelContext;
            }
        };
    }

    @Produces
    @Singleton
    ContentRetriever contentRetriever(EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();
    }
}
