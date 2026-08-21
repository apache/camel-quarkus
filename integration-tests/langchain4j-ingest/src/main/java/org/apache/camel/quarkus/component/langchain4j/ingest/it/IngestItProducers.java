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

import javax.sql.DataSource;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;

@ApplicationScoped
public class IngestItProducers {

    // @Named puts both beans into the Camel registry, where the ingest extension resolves them
    @Produces
    @Singleton
    @Named("products-store")
    EmbeddingStore<TextSegment> productsStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Produces
    @Singleton
    @Named("custom-store")
    EmbeddingStore<TextSegment> customStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Produces
    @Singleton
    @Named("datasheets-store")
    EmbeddingStore<TextSegment> datasheetsStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Produces
    @Singleton
    @Named("s3-store")
    EmbeddingStore<TextSegment> s3Store() {
        return new InMemoryEmbeddingStore<>();
    }

    @Produces
    @Singleton
    @Named("events-store")
    EmbeddingStore<TextSegment> eventsStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Produces
    @Singleton
    @Named("jdbc-store")
    EmbeddingStore<TextSegment> jdbcStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Produces
    @Singleton
    @Named("test-model")
    EmbeddingModel embeddingModel() {
        return new DeterministicEmbeddingModel(64);
    }

    // the custom pipeline's register; auto-create is also set, so this existing bean must win
    @Produces
    @Singleton
    @Named("test-register")
    IdempotentRepository testRegister() {
        return MemoryIdempotentRepository.memoryIdempotentRepository(1000);
    }

    // the JDBC register from the documentation's CDI example, over the Dev Services H2 datasource
    @Produces
    @Singleton
    @Named("jdbcRegister")
    IdempotentRepository jdbcRegister(DataSource dataSource) {
        return new JdbcMessageIdRepository(dataSource, "ingest-jdbc");
    }
}
