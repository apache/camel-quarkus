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
package org.apache.camel.quarkus.core;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import org.apache.camel.spi.BeanRepository;

public final class RuntimeBeanRepository implements BeanRepository {

    private final Map<String, CamelBeanQualifierResolver> beanQualifierResolvers;

    public RuntimeBeanRepository(Map<String, CamelBeanQualifierResolver> beanQualifierResolvers) {
        this.beanQualifierResolvers = beanQualifierResolvers;
    }

    private static <T> Set<Bean<? extends T>> resolveAmbiguity(BeanManager manager, Set<Bean<? extends T>> beans) {
        if (beans.size() > 1) {
            try {
                return Collections.singleton(manager.resolve(beans));
            } catch (AmbiguousResolutionException are) {
                //in case of AmbiguousResolutionException, original collection is returned
            }
        }

        return beans;
    }

    private static <T> Map<String, T> getReferencesByTypeWithName(Class<T> type, Annotation... qualifiers) {
        return getBeanManager()
                .map(manager -> getReferencesByTypeWithName(manager, type, qualifiers))
                .orElseGet(Collections::emptyMap);
    }

    private static <T> Set<T> getReferencesByType(BeanManager manager, Class<T> type, Annotation... qualifiers) {
        Set<T> answer = new HashSet<>();

        for (Bean<?> bean : resolveAmbiguity(manager, manager.getBeans(type, qualifiers))) {
            T ref = getReference(manager, type, bean);
            if (ref != null) {
                answer.add(ref);
            }
        }

        return answer;
    }

    private static <T> Optional<T> getReferenceByName(BeanManager manager, String name, Class<T> type) {
        return Optional.ofNullable(manager.resolve(manager.getBeans(name))).map(bean -> getReference(manager, type, bean));
    }

    private static <T> T getReference(BeanManager manager, Class<T> type, Bean<?> bean) {
        return type.cast(manager.getReference(bean, Object.class, manager.createCreationalContext(bean)));
    }

    private static <T> Map<String, T> getReferencesByTypeWithName(BeanManager manager, Class<T> type,
            Annotation... qualifiers) {
        Map<String, T> answer = new HashMap<>();

        for (Bean<?> bean : manager.getBeans(type, qualifiers)) {

            T ref = getReference(manager, type, bean);
            if (ref != null) {
                answer.put(bean.getName(), ref);
            }
        }

        return answer;
    }

    private static Optional<BeanManager> getBeanManager() {
        ArcContainer container = Arc.container();
        if (container == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(container.beanManager());
    }

    @Override
    public Object lookupByName(String name) {
        return lookupByNameAndType(name, Object.class);
    }

    @Override
    public <T> T lookupByNameAndType(String name, Class<T> type) {
        return getBeanManager()
                .flatMap(manager -> getReferenceByName(manager, name, type))
                .orElse(null);
    }

    @Override
    public <T> Map<String, T> findByTypeWithName(Class<T> type) {
        Optional<Annotation[]> qualifiers = resolveQualifiersForType(type);
        if (qualifiers.isPresent()) {
            return getReferencesByTypeWithName(type, qualifiers.get());
        }
        return getReferencesByTypeWithName(type);
    }

    @Override
    public <T> Set<T> findByType(Class<T> type) {
        Optional<Annotation[]> qualifiers = resolveQualifiersForType(type);
        if (qualifiers.isPresent()) {
            return getBeanManager()
                    .map(manager -> getReferencesByType(manager, type, qualifiers.get()))
                    .orElseGet(Collections::emptySet);
        }
        return getBeanManager()
                .map(manager -> getReferencesByType(manager, type))
                .orElseGet(Collections::emptySet);
    }

    private Optional<Annotation[]> resolveQualifiersForType(Class<?> type) {
        CamelBeanQualifierResolver resolver = beanQualifierResolvers.get(type.getName());
        if (resolver != null) {
            return Optional.ofNullable(resolver.resolveQualifiers());
        }
        return Optional.empty();
    }
}
