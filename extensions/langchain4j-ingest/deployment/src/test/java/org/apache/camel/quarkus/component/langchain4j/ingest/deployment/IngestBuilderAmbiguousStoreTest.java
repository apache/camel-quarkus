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
package org.apache.camel.quarkus.component.langchain4j.ingest.deployment;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import io.quarkus.test.QuarkusExtensionTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.langchain4j.ingest.Ingest;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestPipeline;
import org.apache.camel.quarkus.component.langchain4j.ingest.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * With two store beans, a Java-declared pipeline must name one. The message points at the
 * builder method: the configuration key would be rejected at build time for this name.
 */
class IngestBuilderAmbiguousStoreTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestEmbeddingBeans.class, OtherStore.class, Pipelines.class))
            .assertException(t -> ValidationTestSupport.assertFailure(t,
                    "Ingestion pipeline 'docs' found 2 embedding store beans",
                    "Name the one to use with IngestPipeline.embeddingStore(...) in its @Ingest method"));

    @Test
    void startMustFail() {
        Assertions.fail("The application start was expected to fail");
    }

    @ApplicationScoped
    public static class OtherStore {

        @Produces
        @Singleton
        @Named("other-store")
        EmbeddingStore<TextSegment> store() {
            return new InMemoryEmbeddingStore<>();
        }
    }

    @ApplicationScoped
    public static class Pipelines {

        @Ingest("docs")
        IngestPipeline docs() {
            return IngestPipeline.from(Source.file("target/ambiguous-store")).embeddingModel("model");
        }
    }
}
