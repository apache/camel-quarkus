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
package org.apache.camel.quarkus.component.langchain4j.ragbridge.it;

import dev.langchain4j.rag.RetrievalAugmentor;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/rag-bridge")
public class RagBridgeResource {

    @Inject
    Instance<RetrievalAugmentor> retrievalAugmentorInstance;

    @Inject
    @Named("products")
    Instance<RetrievalAugmentor> namedAugmentorInstance;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    RagAiService aiService;

    @GET
    @Path("/augmentor-present")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isAugmentorPresent() {
        return retrievalAugmentorInstance.isResolvable();
    }

    @GET
    @Path("/named-augmentor-present")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isNamedAugmentorPresent() {
        return namedAugmentorInstance.isResolvable();
    }

    @POST
    @Path("/ingest")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ingest(String text) {
        producerTemplate.sendBody("direct:ingest", text);
        return "ingested";
    }

    @POST
    @Path("/ingest-products")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ingestProducts(String text) {
        producerTemplate.sendBody("direct:ingest-products", text);
        return "ingested";
    }

    @POST
    @Path("/ask")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ask(String question) {
        return aiService.chat(question);
    }
}
