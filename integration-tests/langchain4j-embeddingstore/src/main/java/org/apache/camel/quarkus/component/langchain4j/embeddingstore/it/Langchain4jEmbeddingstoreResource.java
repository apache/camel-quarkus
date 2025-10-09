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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.langchain4j.embeddingstore.LangChain4jEmbeddingStoreAction;
import org.apache.camel.component.langchain4j.embeddingstore.LangChain4jEmbeddingStoreHeaders;

@Path("/langchain4j-embeddingstore")
@ApplicationScoped
public class Langchain4jEmbeddingstoreResource {
    @Inject
    FluentProducerTemplate fluentProducerTemplate;

    @Path("/{embeddingStoreType}/add")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String embeddingStoreAdd(
            @PathParam("embeddingStoreType") String embeddingStoreType,
            String content) {

        return fluentProducerTemplate.to("direct:start")
                .withHeader("embeddingStoreType", embeddingStoreType)
                .withHeader(LangChain4jEmbeddingStoreHeaders.ACTION, LangChain4jEmbeddingStoreAction.ADD)
                .withBody(content)
                .request(String.class);
    }

    @SuppressWarnings("unchecked")
    @Path("/{embeddingStoreType}/search")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, String>> embeddingStoreSearch(
            @PathParam("embeddingStoreType") String embeddingStoreType,
            String content) {

        List<EmbeddingMatch<TextSegment>> matches = fluentProducerTemplate.to("direct:start")
                .withHeader("embeddingStoreType", embeddingStoreType)
                .withHeader(LangChain4jEmbeddingStoreHeaders.ACTION, LangChain4jEmbeddingStoreAction.SEARCH)
                .withBody(content)
                .request(List.class);

        List<Map<String, String>> results = new ArrayList<>();
        matches.forEach(match -> {
            Map<String, String> embeddingInfo = new HashMap<>();
            embeddingInfo.put("embeddingId", match.embeddingId());
            embeddingInfo.put("text", match.embedded().text());
            results.add(embeddingInfo);
        });

        return results;
    }

    @Path("/{embeddingStoreType}/remove")
    @DELETE
    public void embeddingStoreDelete(
            @PathParam("embeddingStoreType") String embeddingStoreType,
            @QueryParam("embeddingId") String embeddingId) {
        fluentProducerTemplate.to("direct:start")
                .withHeader("embeddingStoreType", embeddingStoreType)
                .withHeader(LangChain4jEmbeddingStoreHeaders.ACTION, LangChain4jEmbeddingStoreAction.REMOVE)
                .withBody(embeddingId)
                .request();
    }
}
