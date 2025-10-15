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
package org.apache.camel.quarkus.component.langchain4j.agent.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.camel.component.langchain4j.agent.api.Agent;
import org.apache.camel.component.langchain4j.agent.api.AgentConfiguration;
import org.apache.camel.component.langchain4j.agent.api.AgentWithMemory;
import org.apache.camel.component.langchain4j.agent.api.AgentWithoutMemory;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.TestPojoJsonExtractorOutputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationFailureInputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationFailureOutputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationSuccessInputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationSuccessOutputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.service.TestPojoAiAgent;
import org.apache.camel.quarkus.component.langchain4j.agent.it.tool.AdditionTool;
import org.apache.camel.quarkus.component.langchain4j.agent.it.util.PersistentChatMemoryStore;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static java.time.Duration.ofSeconds;

@ApplicationScoped
public class AgentProducers {
    @ConfigProperty(name = "langchain4j.ollama.base-url")
    String baseUrl;

    @Produces
    @Identifier("ollamaOrcaMiniModel")
    ChatModel ollamaOrcaMiniModel() {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName("orca-mini")
                .temperature(0.3)
                .build();
    }

    @Produces
    @Identifier("ollamaLlama31Model")
    ChatModel ollamaLlama31Model() {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName("llama3.1:latest")
                .temperature(0.3)
                .logResponses(true)
                .logRequests(true)
                .build();
    }

    @Produces
    ChatMemoryStore chatMemoryStore() {
        return new PersistentChatMemoryStore();
    }

    @Produces
    RetrievalAugmentor retrievalAugmentor() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream("rag/company-knowledge-base.txt")) {
            if (stream == null) {
                throw new IllegalArgumentException("company-knowledge.txt not found");
            }

            Document document = Document.from(new String(stream.readAllBytes(), StandardCharsets.UTF_8));

            List<TextSegment> segments = DocumentSplitters.recursive(300, 100).split(document);

            EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                    .baseUrl(baseUrl)
                    .modelName("nomic-embed-text")
                    .timeout(Duration.ofSeconds(30))
                    .build();

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

            // Store in embedding store
            EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
            embeddingStore.addAll(embeddings, segments);

            // Create content retriever
            EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .maxResults(3)
                    .minScore(0.6)
                    .build();

            // Create a RetrievalAugmentor that uses only a content retriever : naive rag scenario
            return DefaultRetrievalAugmentor.builder()
                    .contentRetriever(contentRetriever)
                    .build();
        }
    }

    @Produces
    @Identifier("simpleAgent")
    Agent simpleAgent(@Identifier("ollamaOrcaMiniModel") ChatModel chatModel) {
        return new AgentWithoutMemory(new AgentConfiguration().withChatModel(chatModel));
    }

    @Produces
    @Identifier("agentWithMemory")
    Agent agentWithMemory(@Identifier("ollamaLlama31Model") ChatModel chatModel, ChatMemoryStore chatMemoryStore) {
        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(chatMemoryStore)
                .build();

        return new AgentWithMemory(new AgentConfiguration()
                .withChatModel(chatModel)
                .withChatMemoryProvider(chatMemoryProvider));
    }

    @Produces
    @Identifier("agentWithSuccessInputGuardrail")
    Agent agentWithSuccessInputGuardrail(@Identifier("ollamaOrcaMiniModel") ChatModel chatModel) {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(chatModel)
                .withInputGuardrailClasses(List.of(ValidationSuccessInputGuardrail.class)));
    }

    @Produces
    @Identifier("agentWithFailingInputGuardrail")
    Agent agentWithFailingInputGuardrail(@Identifier("ollamaOrcaMiniModel") ChatModel chatModel) {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(chatModel)
                .withInputGuardrailClasses(List.of(ValidationFailureInputGuardrail.class)));
    }

    @Produces
    @Identifier("agentWithSuccessOutputGuardrail")
    Agent agentWithSuccessOutputGuardrail(@Identifier("ollamaOrcaMiniModel") ChatModel chatModel) {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(chatModel)
                .withOutputGuardrailClasses(List.of(ValidationSuccessOutputGuardrail.class)));
    }

    @Produces
    @Identifier("agentWithFailingOutputGuardrail")
    Agent agentWithFailingOutputGuardrail(@Identifier("ollamaOrcaMiniModel") ChatModel chatModel) {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(chatModel)
                .withOutputGuardrailClasses(List.of(ValidationFailureOutputGuardrail.class)));
    }

    @Produces
    @Identifier("agentWithJsonExtractorOutputGuardrail")
    Agent agentWithJsonExtractorOutputGuardrail(@Identifier("ollamaOrcaMiniModel") ChatModel chatModel) {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(chatModel)
                .withOutputGuardrailClasses(List.of(TestPojoJsonExtractorOutputGuardrail.class)));
    }

    @Produces
    @Identifier("agentWithRag")
    public Agent agentWithRag(
            @Identifier("ollamaOrcaMiniModel") ChatModel chatModel,
            RetrievalAugmentor retrievalAugmentor) {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(chatModel)
                .withRetrievalAugmentor(retrievalAugmentor));
    }

    @Produces
    @Identifier("agentWithTools")
    public Agent agentWithTools(@Identifier("ollamaLlama31Model") ChatModel chatModel) {
        return new AgentWithoutMemory(new AgentConfiguration().withChatModel(chatModel));
    }

    @Produces
    @Identifier("agentWithCustomService")
    public Agent agentCustom(
            @Identifier("ollamaOrcaMiniModel") ChatModel chatModel,
            ObjectMapper objectMapper) {
        return new TestPojoAiAgent(new AgentConfiguration()
                .withChatModel(chatModel), objectMapper);
    }

    @Produces
    @Identifier("agentWithCustomTools")
    Agent agentWithCustomTools(@Identifier("ollamaLlama31Model") ChatModel chatModel) {
        return new AgentWithoutMemory(new AgentConfiguration()
                .withChatModel(chatModel)
                .withCustomTools(List.of(new AdditionTool())));
    }
}
