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
package org.apache.camel.quarkus.component.langchain4j.ingest;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.jboss.logging.Logger;

/**
 * Resolves the beans a pipeline is wired from: embedding stores and models through CDI, and the
 * duplicate registers through the Camel registry.
 */
@Singleton
public class IngestBeanResolver {

    /**
     * The built-in register capacity, sized above Camel's 1000-entry cap so eviction does not
     * re-ingest large directories during normal operation; in-memory, so lost on restart.
     */
    static final int DEFAULT_REGISTER_CAPACITY = 100_000;

    private static final Logger LOG = Logger.getLogger(IngestBeanResolver.class);

    @Inject
    CamelContext camelContext;

    // these injection points also keep an unnamed store or model bean from being removed as
    // unused - nothing else in the application need inject it
    @Inject
    @Any
    Instance<EmbeddingStore<TextSegment>> storeCandidates;

    @Inject
    @Any
    Instance<EmbeddingModel> modelCandidates;

    EmbeddingStore<TextSegment> resolveStore(String name, String configured) {
        return resolve(name, storeCandidates, configured, "embedding store", "embedding-store");
    }

    EmbeddingModel resolveModel(String name, String configured) {
        return resolve(name, modelCandidates, configured, "embedding model", "embedding-model");
    }

    /**
     * Binds an in-memory register under the configured name, unless a bean with that name
     * already exists — {@code camel.beans.*} beans are bound before route builders run, so both
     * they and CDI beans are visible here and win.
     */
    void maybeAutoCreateRepository(String name, IngestRunTimeConfig.PipelineRunTimeConfig runtime) {
        if (runtime == null || !runtime.source().idempotentRepositoryAutoCreate()) {
            return;
        }
        String repositoryName = runtime.source().idempotentRepository().orElse(null);
        if (repositoryName == null) {
            throw new IllegalStateException("Ingestion pipeline '" + name
                    + "' sets source.idempotent-repository-auto-create but no "
                    + "source.idempotent-repository name to create the register under.");
        }
        if (camelContext.getRegistry().lookupByNameAndType(repositoryName, IdempotentRepository.class) != null) {
            LOG.infof("Ingestion pipeline '%s': idempotent repository '%s' already exists, auto-create skipped",
                    name, repositoryName);
            return;
        }
        camelContext.getRegistry().bind(repositoryName,
                MemoryIdempotentRepository.memoryIdempotentRepository(DEFAULT_REGISTER_CAPACITY));
    }

    /**
     * Resolves the configured register from the Camel registry: CDI producers, camel.beans
     * definitions and auto-created registers alike. By name only — the application may hold
     * unrelated idempotent repositories.
     */
    IdempotentRepository resolveRepository(String name, String repositoryName) {
        IdempotentRepository repository = camelContext.getRegistry().lookupByNameAndType(repositoryName,
                IdempotentRepository.class);
        if (repository == null) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' references idempotent repository '"
                    + repositoryName + "' but no such bean exists");
        }
        // a CDI-produced repository does not pass through the registry's bind hook, so a
        // CamelContextAware implementation would otherwise run contextless
        CamelContextAware.trySetCamelContext(repository, camelContext);
        return repository;
    }

    /**
     * CDI is the one mechanism for both lookups: the named path selects on the qualifier, the
     * unnamed path counts the candidates — through handles, so beans are not instantiated merely
     * to be counted. Picking one silently would bind a pipeline to whichever bean happened to be
     * discovered first. A raw-typed registry search cannot serve here: it never matches a bean
     * typed {@code EmbeddingStore<TextSegment>}.
     */
    private <T> T resolve(String name, Instance<T> candidates, String configured, String what, String property) {
        if (configured != null) {
            Instance<T> named = candidates.select(NamedLiteral.of(configured));
            if (named.isUnsatisfied()) {
                throw new IllegalStateException("Ingestion pipeline '" + name + "' references " + what + " '"
                        + configured + "' but no such bean exists");
            }
            return named.get();
        }
        List<Instance.Handle<T>> handles = StreamSupport.stream(candidates.handles().spliterator(), false)
                .collect(Collectors.toList());
        if (handles.isEmpty()) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' needs an " + what
                    + ", but no bean of that type exists. Define one, for example with a @Produces method.");
        }
        if (handles.size() > 1) {
            throw new IllegalStateException("Ingestion pipeline '" + name + "' found " + handles.size() + " "
                    + what + " beans. Name the one to use with quarkus.camel.langchain4j.ingest." + name + "."
                    + property);
        }
        return handles.get(0).get();
    }
}
