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
package org.apache.camel.quarkus.component.langchain4j.embeddings.ql4j.it;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.langchain4j.data.embedding.Embedding;
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

@Path("/langchain4j-embeddings")
@ApplicationScoped
public class Langchain4jEmbeddingsQl4jResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEmbedding(String text) throws Exception {
        Exchange result = producerTemplate.request("direct:start", e -> e.getMessage().setBody(text));
        Message message = result.getMessage();

        Embedding embedding = message.getHeader(LangChain4jEmbeddingsHeaders.VECTOR, Embedding.class);
        Integer inputTokenLength = message.getHeader(LangChain4jEmbeddingsHeaders.INPUT_TOKEN_COUNT, Integer.class);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vectorLength", embedding.vector().length);
        if (inputTokenLength != null) {
            body.put("inputTokenLength", inputTokenLength);
        }
        return Response.ok().entity(body).build();
    }
}
