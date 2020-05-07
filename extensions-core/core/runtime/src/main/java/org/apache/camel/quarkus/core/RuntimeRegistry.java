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

import java.util.*;
import java.util.function.BiConsumer;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.Registry;

public class RuntimeRegistry implements Registry, CamelContextAware {

    protected CamelContext camelContext;
    protected Map<String, Map<Class<?>, Object>> priority = new LinkedHashMap<>();
    protected Map<String, Map<Class<?>, Object>> fallback = new LinkedHashMap<>();
    protected Map<Class<?>, LazyProxy<?>> lazyProxies = new HashMap<>();

    public Map<Class<?>, LazyProxy<?>> getLazyProxyClasses() {
        return lazyProxies;
    }

    public void addLazyProxy(Class<?> type, Object proxy) {
        type.cast(proxy);
        lazyProxies.put(type, LazyProxy.class.cast(proxy));
    }

    public void startProxies() {
        BeanManager manager = Arc.container().beanManager();
        for (Map.Entry<Class<?>, LazyProxy<?>> p : lazyProxies.entrySet()) {
            Bean<?> bean = manager.resolve(manager.getBeans(p.getKey()));
            Object delegate = getReference(manager, p.getKey(), bean);
            ((LazyProxy<Object>) p.getValue()).setDelegate(delegate);
        }
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public void bind(String name, Class<?> type, Object bean) throws RuntimeCamelException {
        bind(name, type, bean, false);
    }

    public void bind(String name, Class<?> type, Object instance, boolean priority) {
        (priority ? this.priority : fallback)
                .computeIfAbsent(name, k -> new LinkedHashMap<>()).put(type, wrap(instance));
    }

    @Override
    public Object lookupByName(String name) {
        return lookupByNameAndType(name, Object.class);
    }

    @Override
    public <T> T lookupByNameAndType(String name, Class<T> type) {
        // Must avoid attempting placeholder resolution when looking up
        // the properties component or else we end up in an infinite loop.
        if (camelContext != null && !name.equals("properties")) {
            name = camelContext.resolvePropertyPlaceholders(name);
        }
        T answer = doLookupByNameAndType(priority, name, type);
        if (answer == null) {
            BeanManager manager = Arc.container().beanManager();
            answer = doLookupByNameAndType(manager, name, type);
        }
        if (answer == null) {
            answer = doLookupByNameAndType(fallback, name, type);
        }
        return answer;
    }

    @Override
    public <T> Map<String, T> findByTypeWithName(Class<T> type) {
        Map<String, T> answer = new LinkedHashMap<>();
        doFindByType(type, answer::put);
        return answer;
    }

    @Override
    public <T> Set<T> findByType(Class<T> type) {
        Set<T> answer = new LinkedHashSet<>();
        doFindByType(type, (n, v) -> answer.add(v));
        return answer;
    }

    @Override
    public Object unwrap(Object value) {
        return (value instanceof RuntimeValue)
                ? ((RuntimeValue<?>) value).getValue()
                : value;
    }

    protected <T> T doLookupByNameAndType(Map<String, Map<Class<?>, Object>> reg, String name, Class<T> type) {
        Map<Class<?>, Object> map = reg.get(name);
        if (map == null) {
            return null;
        }
        Object answer = map.get(type);
        if (answer != null) {
            return type.cast(unwrap(answer));
        }
        // look for first entry that is the type
        for (Object value : map.values()) {
            value = unwrap(value);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return null;
    }

    protected <T> T doLookupByNameAndType(BeanManager manager, String name, Class<T> type) {
        Bean<?> bean = manager.resolve(manager.getBeans(name));
        if (bean != null) {
            return getReference(manager, type, bean);
        }
        return null;
    }

    protected <T> void doFindByType(Class<T> type, BiConsumer<String, T> collector) {
        if (lazyProxies.containsKey(type)) {
            collector.accept(type.toString(), type.cast(lazyProxies.get(type)));
        } else if (!doFindByType(priority, type, collector)) {
            BeanManager manager = Arc.container().beanManager();
            doFindByType(manager, type, collector);
            doFindByType(fallback, type, collector);
        }
    }

    protected <T> boolean doFindByType(
            Map<String, Map<Class<?>, Object>> reg, Class<T> type,
            BiConsumer<String, T> collector) {
        boolean found = false;
        for (Map.Entry<String, Map<Class<?>, Object>> entry : reg.entrySet()) {
            for (Object value : entry.getValue().values()) {
                value = unwrap(value);
                if (type.isInstance(value)) {
                    collector.accept(entry.getKey(), type.cast(value));
                    found = true;
                }
            }
        }
        return found;
    }

    private <T> void doFindByType(BeanManager manager, Class<T> type, BiConsumer<String, T> collector) {
        for (Bean<?> bean : manager.getBeans(type)) {
            T ref = getReference(manager, type, bean);
            collector.accept(bean.getName(), ref);
        }
    }

    private <T> T getReference(BeanManager manager, Class<T> type, Bean<?> bean) {
        return type.cast(manager.getReference(bean, type, manager.createCreationalContext(bean)));
    }

}
