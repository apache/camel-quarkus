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

import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.filter.Filter;

/**
 * The retrieval-side isolation hook: implemented as a CDI bean, consulted on <em>every</em>
 * retrieval performed by a {@link dev.langchain4j.rag.RetrievalAugmentor} produced by this
 * extension. Segment metadata written at ingestion time (a tenant key, a classification, an
 * owner) isolates nothing on its own; this is what turns it into an access control.
 *
 * <p>
 * Typical implementation: derive the caller's tenant from the request and return
 * {@code metadataKey("tenant").isEqualTo(tenant)}.
 *
 * <p>
 * At most one implementation may exist and it must not be {@code @Dependent} — both are build
 * failures. Declare it {@code @ApplicationScoped}, {@code @RequestScoped} or {@code @Singleton};
 * it is resolved once per augmentor, so per-request state belongs in an injected request-scoped
 * bean rather than in a field.
 *
 * <p>
 * <strong>The filter is only as strong as the store.</strong> {@code EmbeddingStore}
 * implementations are not required to support filtering, and one that ignores the filter returns
 * every match. Verify against the store actually in use before relying on this for isolation.
 */
public interface RagRetrievalFilterSupplier {

    /**
     * @param  query              the retrieval query, whose {@link Query#metadata()} carries the
     *                            {@code chatMemoryId} of the calling AI service when there is one
     * @param  augmentorName      the name of the augmentor performing the retrieval, as configured
     *                            under {@code quarkus.camel.langchain4j.rag.augmentors.<name>};
     *                            {@code null} for the auto-produced default augmentor
     * @param  embeddingStoreName the store being searched, named by
     *                            {@code quarkus.camel.langchain4j.rag.augmentors.<name>.embedding-store-name};
     *                            {@code null} when the augmentor is backed by the {@code @Default}
     *                            store. The same supplier serves every produced augmentor, so this
     *                            is what tells a filter written for one store's metadata whether it
     *                            applies at all — return {@code null} for the stores it does not
     *                            know, since filtering on an absent metadata key matches nothing.
     * @return                    the filter to apply, or {@code null} for unfiltered retrieval, as
     *                            in LangChain4j itself. {@code null} therefore means "no
     *                            restriction", never "deny": an implementation that cannot
     *                            determine the caller must throw, so that retrieval fails instead
     *                            of returning every tenant's documents.
     */
    Filter filter(Query query, String augmentorName, String embeddingStoreName);
}
