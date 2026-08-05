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

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import io.quarkiverse.langchain4j.EmbeddingStoreName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@ApplicationScoped
public class RagBridgeProducers {

    // @Named registers the bean in the Camel registry so the langchain4j-embeddingstore
    // endpoint can reference it via ?embeddingStore=#defaultStore (autowiring alone is
    // ambiguous when multiple EmbeddingStore beans exist)
    @Produces
    @Singleton
    @Named("defaultStore")
    EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    // @Named makes this store addressable in Camel URIs as #products;
    // @EmbeddingStoreName is the Quarkus LangChain4j qualifier used by the
    // RAG bridge's DefaultRetrievalAugmentorSupplier to resolve the store at runtime
    @Produces
    @Singleton
    @Named("products")
    @EmbeddingStoreName("products")
    EmbeddingStore<TextSegment> productsEmbeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    // Deliberately carries no @Named qualifier. CDI @Named beans end up in the Camel
    // registry on their own, so a store that has both qualifiers cannot tell us whether
    // the @EmbeddingStoreName registry bridge did anything. This one is only reachable
    // through the bridge.
    @Produces
    @Singleton
    @EmbeddingStoreName("qualifier-only")
    EmbeddingStore<TextSegment> qualifierOnlyEmbeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Produces
    @Singleton
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
