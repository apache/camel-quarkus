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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.guardrail.Guardrail;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.TypeLiteral;
import org.apache.camel.spi.Registry;
import org.jboss.logging.Logger;

@Recorder
public class QuarkusLangchain4jRecorder {

    private static final Logger LOG = Logger.getLogger(QuarkusLangchain4jRecorder.class);
    private static final String EMBEDDING_STORE_NAME_CLASS = "io.quarkiverse.langchain4j.EmbeddingStoreName";
    @SuppressWarnings("serial")
    private static final TypeLiteral<EmbeddingStore<TextSegment>> EMBEDDING_STORE_TYPE = new TypeLiteral<>() {
    };

    public void setCamelAiToolTagMap(Map<String, String> tagMap) {
        CamelAiToolProvider.TAG_MAP.putAll(tagMap);
    }

    public void registerNamedEmbeddingStores(RuntimeValue<Registry> camelRegistry) {
        List<InstanceHandle<EmbeddingStore<TextSegment>>> handles = Arc.container()
                .listAll(EMBEDDING_STORE_TYPE, Any.Literal.INSTANCE);

        Registry registry = camelRegistry.getValue();
        for (InstanceHandle<EmbeddingStore<TextSegment>> handle : handles) {
            Bean<?> bean = handle.getBean();
            if (bean == null) {
                continue;
            }
            for (Annotation qualifier : bean.getQualifiers()) {
                if (EMBEDDING_STORE_NAME_CLASS.equals(qualifier.annotationType().getName())) {
                    try {
                        String name = (String) qualifier.annotationType()
                                .getMethod("value").invoke(qualifier);
                        if (name != null && !name.isEmpty()) {
                            // The identity check keeps a bean carrying both @Named("x") and
                            // @EmbeddingStoreName("x") from triggering a spurious warning
                            EmbeddingStore<?> existing = registry.lookupByNameAndType(name, EmbeddingStore.class);
                            if (existing != null && existing != handle.get()) {
                                LOG.warnf(
                                        "The Camel registry already resolves a different EmbeddingStore under the name \"%s\"; lookups by that name may not return the @EmbeddingStoreName(\"%s\") bean",
                                        name, name);
                            }
                            // Bind via supplier so a store never referenced by a route is not instantiated
                            registry.bind(name, EmbeddingStore.class, handle::get);
                            LOG.debugf("Registered @EmbeddingStoreName(\"%s\") in Camel registry", name);
                        }
                    } catch (ReflectiveOperationException e) {
                        LOG.warnf(e, "Failed to extract name from @EmbeddingStoreName qualifier");
                    }
                }
            }
        }
    }

    public RuntimeValue<Guardrail<?, ?>> instantiateGuardrails(Class<Guardrail<?, ?>> guardrailClass) {
        try {
            return new RuntimeValue<>(guardrailClass.getConstructor().newInstance());
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            Logger.getLogger(QuarkusLangchain4jRecorder.class).debugf(e,
                    "Can not instantiate guardrail of class %s", guardrailClass.getName());
            return null;
        }
    }

    public Supplier<RetrievalAugmentor> createDefaultRetrievalAugmentorSupplier(
            String embeddingStoreName, String embeddingModelName) {
        return new DefaultRetrievalAugmentorSupplier(embeddingStoreName, embeddingModelName);
    }
}
