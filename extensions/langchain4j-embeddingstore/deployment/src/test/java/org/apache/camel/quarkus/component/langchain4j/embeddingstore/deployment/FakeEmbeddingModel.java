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
package org.apache.camel.quarkus.component.langchain4j.embeddingstore.deployment;

import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.enterprise.context.ApplicationScoped;

/** A deterministic stand-in for an embedding model: equal texts get equal vectors. */
@ApplicationScoped
public class FakeEmbeddingModel implements EmbeddingModel {

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        List<Embedding> embeddings = new ArrayList<>(segments.size());
        for (TextSegment segment : segments) {
            int hash = segment.text().hashCode();
            float[] vector = new float[8];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = ((hash >>> (i * 4)) & 0xF) + 1f;
            }
            embeddings.add(Embedding.from(vector));
        }
        return Response.from(embeddings);
    }
}
