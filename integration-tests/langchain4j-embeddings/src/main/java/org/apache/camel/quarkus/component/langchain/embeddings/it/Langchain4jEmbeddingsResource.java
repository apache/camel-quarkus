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
package org.apache.camel.quarkus.component.langchain.embeddings.it;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import io.quarkus.vertx.http.runtime.devmode.Json;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.langchain4j.embeddings.LangChain4jEmbeddingsHeaders;
import org.apache.camel.support.LRUCacheFactory;

@Path("/langchain4j-embeddings")
@ApplicationScoped
public class Langchain4jEmbeddingsResource {
    @Inject
    ProducerTemplate producerTemplate;

    @jakarta.enterprise.inject.Produces
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Path("/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEmbedding(String text) throws Exception {
        try {
            Exchange result = producerTemplate.request("direct:start", e -> e.getMessage().setBody(text));
            Message message = result.getMessage();

            Embedding embedding = message.getHeader(LangChain4jEmbeddingsHeaders.VECTOR, Embedding.class);
            Integer inputTokenLength = message.getHeader(LangChain4jEmbeddingsHeaders.INPUT_TOKEN_COUNT, Integer.class);

            return Response.ok()
                    .entity(Json.object()
                            .put("vectorLength", embedding.vector().length)
                            .put("inputTokenLength", inputTokenLength)
                            .build())
                    .build();
        } finally {
            LRUCacheFactory.setLRUCacheFactory(null);
        }
    }
}
