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

import java.util.List;
import java.util.stream.Collectors;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The minimal arrangement usage.adoc documents: exactly one store and one model bean, neither
 * named, resolved by type alone. The store bean's parameterized type
 * ({@code EmbeddingStore<TextSegment>}) and the absence of any other injection point make this
 * the regression test for the unnamed lookup — a raw-typed registry search finds no such bean,
 * and without the extension's own {@code Instance} injection points the unused producers would
 * be removed altogether.
 */
class IngestUnnamedBeansTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(UnnamedEmbeddingBeans.class))
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs.source.directory", "target/unnamed-docs");

    @Inject
    CamelContext context;

    @Test
    void unnamedStoreAndModelResolve() {
        Assertions.assertNotNull(context.getRoute("camel-quarkus-langchain4j-ingest-docs"),
                "the pipeline must start with the only store and model beans, unnamed");
    }

    @ApplicationScoped
    public static class UnnamedEmbeddingBeans {

        @Produces
        @Singleton
        EmbeddingStore<TextSegment> store() {
            return new InMemoryEmbeddingStore<>();
        }

        @Produces
        @Singleton
        EmbeddingModel model() {
            return new EmbeddingModel() {
                @Override
                public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
                    return Response.from(segments.stream()
                            .map(segment -> Embedding.from(new float[] { 1f }))
                            .collect(Collectors.toList()));
                }
            };
        }
    }
}
