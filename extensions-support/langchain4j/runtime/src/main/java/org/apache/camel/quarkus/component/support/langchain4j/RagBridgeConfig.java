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
package org.apache.camel.quarkus.component.support.langchain4j;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

// BUILD_AND_RUN_TIME_FIXED: augmentor bean definitions are baked in at build time and cannot change at runtime
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.camel.langchain4j.rag")
public interface RagBridgeConfig {

    /**
     * Named RetrievalAugmentor beans, each backed by a specific EmbeddingStore.
     * The map key becomes the CDI bean name ({@code @Named("key")}).
     *
     * Example:
     *
     * <pre>
     * quarkus.camel.langchain4j.rag.augmentors.products.embedding-store-name=products
     * quarkus.camel.langchain4j.rag.augmentors.support.embedding-store-name=support-docs
     * </pre>
     */
    Map<String, AugmentorConfig> augmentors();

    interface AugmentorConfig {

        /**
         * CDI bean name of the {@code EmbeddingStore} to use.
         * Matches beans annotated with {@code @Named("name")} or {@code @EmbeddingStoreName("name")}.
         */
        String embeddingStoreName();

        /**
         * CDI bean name of the {@code EmbeddingModel} to use.
         * When not set, the default (unnamed) EmbeddingModel is used.
         */
        Optional<String> embeddingModelName();

        /**
         * Marks this augmentor as the one {@code @RegisterAiService} AI services use when they
         * do not select an augmentor explicitly. Exactly one augmentor must be marked when more
         * than one is configured — otherwise the build fails, because an unmarked ambiguity
         * would silently disable RAG for every AI service in the application.
         */
        @WithName("default")
        @WithDefault("false")
        boolean defaultAugmentor();
    }
}
