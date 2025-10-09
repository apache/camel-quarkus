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
package org.apache.camel.quarkus.component.langchain4j.embeddingstore.it;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.apache.camel.CamelContext;
import org.apache.camel.component.langchain4j.embeddingstore.EmbeddingStoreFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Langchain4jEmbeddingstoreProducers {
    @ConfigProperty(name = "qdrant.host")
    String qdrantHost;

    @ConfigProperty(name = "qdrant.port")
    Integer qdrantPort;

    @ConfigProperty(name = "qdrant.collection")
    String qdrantCollection;

    @Produces
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Singleton
    EmbeddingStoreFactory embeddingStoreFactory() {
        return new EmbeddingStoreFactory() {
            CamelContext camelContext;

            @Override
            public EmbeddingStore<TextSegment> createEmbeddingStore() {
                return QdrantEmbeddingStore.builder()
                        .host(qdrantHost)
                        .port(qdrantPort)
                        .collectionName(qdrantCollection)
                        .build();
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
}
