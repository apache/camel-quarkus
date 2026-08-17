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

import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.quarkus.component.support.langchain4j.RagRetrievalFilterSupplier;

/**
 * The retrieval-side isolation hook under test: every retrieval through a produced augmentor is
 * filtered to one tenant's documents. The tenant comes either from state set out of band (via
 * REST) or from the query's {@code chatMemoryId} — the second source is the one that keeps
 * working when retrieval runs off the caller's thread, so both are exercised.
 *
 * <p>
 * The bean stays inert until a test switches it on: this is the only implementation in the
 * application, so a filter applied by default would silently strip the context out of every
 * other test's AI service call.
 */
@ApplicationScoped
public class TestTenantFilterSupplier implements RagRetrievalFilterSupplier {

    /** Set to this instead of a tenant to take the tenant from the query's chat memory id. */
    static final String FROM_QUERY = "from-query";
    /** Tells "the hook ran and was handed null" apart from "the hook never ran". */
    static final String NOT_CALLED = "<not-called>";

    static volatile String tenant;
    static volatile String lastAugmentorName;
    static volatile String lastEmbeddingStoreName;

    static void forgetLastCall() {
        lastAugmentorName = NOT_CALLED;
        lastEmbeddingStoreName = NOT_CALLED;
    }

    @Override
    public Filter filter(Query query, String augmentorName, String embeddingStoreName) {
        lastAugmentorName = augmentorName;
        lastEmbeddingStoreName = embeddingStoreName;
        if (tenant == null) {
            return null;
        }
        String effectiveTenant = FROM_QUERY.equals(tenant) ? chatMemoryId(query) : tenant;
        return effectiveTenant == null
                ? null
                : MetadataFilterBuilder.metadataKey("cq_tenant").isEqualTo(effectiveTenant);
    }

    private static String chatMemoryId(Query query) {
        Object memoryId = query.metadata() == null ? null : query.metadata().chatMemoryId();
        return memoryId == null ? null : memoryId.toString();
    }
}
