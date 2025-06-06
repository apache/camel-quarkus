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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
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
        Set<Bean<?>> beans = manager.getBeans(name);

        // If it is a Synthetic bean, it should match with type
        beans = beans.stream()
                .filter(bean -> {
                    if (bean instanceof InjectableBean injectableBean) {
                        return !injectableBean.getKind().equals(InjectableBean.Kind.SYNTHETIC)
                                || bean.getTypes().contains(type);
                    } else {
                        return true;
                    }
                }).collect(Collectors.toSet());

        if (beans.isEmpty()) {
            // Fallback to searching explicitly with NamedLiteral
            beans = manager.getBeans(type, NamedLiteral.of(name));
        }

        if (beans.isEmpty()) {
            // Fallback to SmallRye @Identifier
            beans = manager.getBeans(type, Identifier.Literal.of(name));
        }

        return Optional.ofNullable(manager.resolve(beans)).map(bean -> getReference(manager, type, bean));
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

    @Override
    public <T> T findSingleByType(Class<T> type) {
        ArcContainer container = Arc.container();
        Optional<Annotation[]> qualifiers = resolveQualifiersForType(type);
        List<InstanceHandle<T>> handles;
        if (qualifiers.isPresent()) {
            handles = container.listAll(type, qualifiers.get());
        } else {
            handles = container.listAll(type);
        }

        if (handles.isEmpty()) {
            // No matches for the given bean type
            return null;
        } else if (handles.size() == 1) {
            // Only 1 bean exists for the given type so just return it
            return handles.get(0).get();
        }

        // For multiple bean matches determine how many have the @Default qualifier
        long defaultBeanCount = handles.stream()
                .map(InstanceHandle::getBean)
                .filter(this::isDefaultBean)
                .count();

        // Determine if all beans for the matching type have the same priority
        boolean beansHaveSamePriority = handles.stream()
                .map(InstanceHandle::getBean)
                .map(InjectableBean::getPriority)
                .distinct()
                .count() == 1;

        // Try to resolve the target bean by @Priority and @Default qualifiers
        if ((defaultBeanCount == 1) || (defaultBeanCount > 1 && !beansHaveSamePriority)) {
            List<InstanceHandle<T>> sortedHandles = new ArrayList<>(handles.size());
            sortedHandles.addAll(handles);
            sortedHandles.sort((bean1, bean2) -> {
                Integer priority2 = bean2.getBean().getPriority();
                Integer priority1 = bean1.getBean().getPriority();

                int result = priority2.compareTo(priority1);
                // If the priority is same, the default bean wins
                if (result == 0) {
                    if (isDefaultBean(bean1.getBean())) {
                        result = -1;
                    } else if (isDefaultBean(bean2.getBean())) {
                        result = 1;
                    }
                }
                return result;
            });
            return sortedHandles.get(0).get();
        }

        // Multiple beans exist for the given type, and we could not determine which one to use
        // Users must resolve the conflict by explicitly referencing the bean via endpoint URI options etc
        return null;
    }

    private Optional<Annotation[]> resolveQualifiersForType(Class<?> type) {
        CamelBeanQualifierResolver resolver = beanQualifierResolvers.get(type.getName());
        if (resolver != null) {
            return Optional.ofNullable(resolver.resolveQualifiers());
        }
        return Optional.empty();
    }

    private boolean isDefaultBean(InjectableBean<?> bean) {
        return bean.getQualifiers().stream().anyMatch(q -> q.annotationType().equals(Default.class));
    }
}
