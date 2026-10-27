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
import java.util.Set;
import java.util.stream.Collectors;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import io.quarkus.test.QuarkusExtensionTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * A media document is embedded whole, so the splitter sizes and the batch size do not apply: settings copied from a
 * text pipeline are not validated, neither at build time nor by the component, and the pipeline starts.
 */
class IngestMediaSplitterSettingsTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MediaBeans.class))
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.clips.source.directory", "target/media-clips")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.clips.modality", "media")
            // both invalid for text: an overlap above the default segment size of 500, and no batch
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.clips.max-overlap-size", "600")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.clips.embedding-batch-size", "0");

    @Inject
    CamelContext context;

    @Test
    void mediaPipelineStarts() {
        Assertions.assertNotNull(context.getRoute("langchain4j-ingest-clips"));
    }

    @ApplicationScoped
    public static class MediaBeans {

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

                @Override
                public Set<ContentType> supportedContentTypes() {
                    return Set.of(ContentType.AUDIO);
                }
            };
        }
    }
}
