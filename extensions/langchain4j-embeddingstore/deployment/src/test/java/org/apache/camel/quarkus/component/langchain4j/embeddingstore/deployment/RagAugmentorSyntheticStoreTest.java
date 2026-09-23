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

import java.util.List;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkiverse.langchain4j.deployment.EmbeddingStoreBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.test.QuarkusExtensionTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.ParameterizedType;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The auto-detected default augmentor must see a store that Quarkus LangChain4j registers: such a store is a
 * synthetic bean, invisible to bean discovery, announced through the store extension's build item.
 */
class RagAugmentorSyntheticStoreTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .addBuildChainCustomizer(builder -> builder.addBuildStep(context -> {
                // what a Quarkus LangChain4j store extension such as pgvector does
                context.produce(SyntheticBeanBuildItem.configure(SyntheticStore.class)
                        .types(ClassType.create(EmbeddingStore.class),
                                ParameterizedType.create(EmbeddingStore.class, ClassType.create(TextSegment.class)))
                        .scope(ApplicationScoped.class)
                        .defaultBean()
                        .unremovable()
                        .creator(SyntheticStoreCreator.class)
                        .done());
                context.produce(new EmbeddingStoreBuildItem());
            })
                    .produces(SyntheticBeanBuildItem.class)
                    .produces(EmbeddingStoreBuildItem.class)
                    .build())
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FakeEmbeddingModel.class, SyntheticStore.class, SyntheticStoreCreator.class));

    @Inject
    Instance<RetrievalAugmentor> augmentor;

    @Inject
    EmbeddingStore<TextSegment> store;

    @Inject
    EmbeddingModel model;

    @Test
    void defaultAugmentorRetrievesFromTheSyntheticStore() {
        assertTrue(augmentor.isResolvable(), "a default RetrievalAugmentor must be produced for the synthetic store");

        String text = "The warranty period is twenty four months.";
        store.add(model.embed(text).content(), TextSegment.from(text));

        UserMessage question = UserMessage.from(text);
        AugmentationResult result = augmentor.get()
                .augment(new AugmentationRequest(question, Metadata.from(question, "test", List.of())));
        assertEquals(1, result.contents().size(), "the augmentor must retrieve from the synthetic store");
        assertEquals(text, result.contents().get(0).textSegment().text());
    }
}
