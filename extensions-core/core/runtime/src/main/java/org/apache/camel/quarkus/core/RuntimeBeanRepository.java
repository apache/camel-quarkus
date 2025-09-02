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
import java.util.HashMap;
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
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.BeanManager;
import org.apache.camel.spi.BeanRepository;
import org.apache.camel.util.ObjectHelper;

public final class RuntimeBeanRepository implements BeanRepository {
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
    private final Map<BeanQualifierResolverIdentifier, CamelBeanQualifierResolver> beanQualifierResolvers;

    public RuntimeBeanRepository(Map<BeanQualifierResolverIdentifier, CamelBeanQualifierResolver> beanQualifierResolvers) {
        this.beanQualifierResolvers = beanQualifierResolvers;
    }

    @Override
    public Object lookupByName(String name) {
        return lookupByNameAndType(name, Object.class);
    }

    @Override
    public <T> T lookupByNameAndType(String name, Class<T> type) {
        return getBeanManager()
                .flatMap(manager -> getReferenceByName(name, type, resolveQualifiersForTypeAndName(type, name)))
                .orElse(null);
    }

    @Override
    public <T> Map<String, T> findByTypeWithName(Class<T> type) {
        return getReferencesByTypeWithName(type, resolveQualifiersForType(type));
    }

    @Override
    public <T> Set<T> findByType(Class<T> type) {
        return Arc.container()
                .listAll(type)
                .stream()
                .filter(InstanceHandle::isAvailable)
                .map(InstanceHandle::get)
                .collect(Collectors.toSet());
    }

    @Override
    public <T> T findSingleByType(Class<T> type) {
        List<InstanceHandle<T>> handles = Arc.container().listAll(type, resolveQualifiersForType(type));
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

    private static <T> Map<String, T> getReferencesByTypeWithName(Class<T> type, Annotation... qualifiers) {
        Map<String, T> beans = new HashMap<>();
        Arc.container()
                .listAll(type, qualifiers)
                .stream()
                .filter(InstanceHandle::isAvailable)
                .forEach(instanceHandle -> {
                    String name = resolveBeanName(instanceHandle.getBean());
                    if (ObjectHelper.isNotEmpty(name)) {
                        beans.put(name, instanceHandle.get());
                    }
                });
        return beans;
    }

    private static <T> Optional<T> getReferenceByName(String name, Class<T> type, Annotation... qualifiers) {
        InstanceHandle<T> instance;
        T result = null;

        if (qualifiers.length == 0) {
            // Try to resolve directly by name
            instance = Arc.container().instance(name);
        } else {
            // If there are qualifiers then one of their attributes may represent the name we want to resolve
            instance = Arc.container().instance(type, qualifiers);
        }

        if (instance.isAvailable()) {
            // If it is a Synthetic bean, it must match with the desired type
            InjectableBean<T> bean = instance.getBean();
            if (bean.getKind().equals(InjectableBean.Kind.SYNTHETIC)) {
                if (bean.getTypes().contains(type)) {
                    result = instance.get();
                }
            } else {
                if (type.isInstance(instance.get())) {
                    result = instance.get();
                }
            }
        }

        if (result == null) {
            // Fallback to searching explicitly with NamedLiteral
            instance = Arc.container().instance(type, NamedLiteral.of(name));
            if (instance.isAvailable()) {
                result = instance.get();
            }
        }

        if (result == null) {
            // Fallback to SmallRye @Identifier
            instance = Arc.container().instance(type, Identifier.Literal.of(name));
            if (instance.isAvailable()) {
                result = instance.get();
            }
        }

        return Optional.ofNullable(result);
    }

    private static Optional<BeanManager> getBeanManager() {
        ArcContainer container = Arc.container();
        if (container == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(container.beanManager());
    }

    private Annotation[] resolveQualifiersForType(Class<?> type) {
        return resolveQualifiersForTypeAndName(type, null);
    }

    private Annotation[] resolveQualifiersForTypeAndName(Class<?> type, String beanName) {
        BeanQualifierResolverIdentifier identifier = BeanQualifierResolverIdentifier.of(type.getName(), beanName);
        CamelBeanQualifierResolver resolver = beanQualifierResolvers.get(identifier);
        if (resolver != null) {
            return resolver.resolveQualifiers();
        }
        return EMPTY_ANNOTATIONS;
    }

    private boolean isDefaultBean(InjectableBean<?> bean) {
        return bean.getQualifiers().stream().anyMatch(q -> q.annotationType().equals(Default.class));
    }

    private static String resolveBeanName(InjectableBean<?> bean) {
        String name = bean.getName();
        if (name == null) {
            for (Annotation qualifier : bean.getQualifiers()) {
                if (qualifier instanceof Identifier) {
                    name = ((Identifier) qualifier).value();
                    break;
                }
            }
        }
        return name;
    }
}
