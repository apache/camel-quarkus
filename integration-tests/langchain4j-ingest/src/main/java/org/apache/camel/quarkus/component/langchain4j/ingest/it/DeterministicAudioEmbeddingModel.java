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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.model.embedding.request.EmbeddingInput;
import dev.langchain4j.model.embedding.request.EmbeddingRequest;
import dev.langchain4j.model.embedding.response.EmbeddingResponse;

/**
 * Deterministic fake that also accepts audio: same bytes, same vector — no network, no model download, native-friendly.
 */
public class DeterministicAudioEmbeddingModel extends DeterministicEmbeddingModel {

    public DeterministicAudioEmbeddingModel(int dimension) {
        super(dimension);
    }

    @Override
    public Set<ContentType> supportedContentTypes() {
        return Set.of(ContentType.TEXT, ContentType.AUDIO);
    }

    @Override
    public EmbeddingResponse doEmbed(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>(request.inputs().size());
        for (EmbeddingInput input : request.inputs()) {
            AudioContent audio = audioOf(input);
            embeddings.add(embeddingFor(audio == null ? input.text() : audio.audio().base64Data()));
        }
        return EmbeddingResponse.builder().embeddings(embeddings).build();
    }

    /** The vector this fake produces for the given bytes: what a query by the same clip embeds to. */
    public Embedding embeddingOf(byte[] audio) {
        return embeddingFor(Base64.getEncoder().encodeToString(audio));
    }

    private static AudioContent audioOf(EmbeddingInput input) {
        if (input.contents() == null) {
            return null;
        }
        for (Content content : input.contents()) {
            if (content instanceof AudioContent audio) {
                return audio;
            }
        }
        return null;
    }
}
