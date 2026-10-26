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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.engine.DefaultFactoryFinderResolver;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.spi.FactoryFinder;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.support.ObjectHelper;
import org.jboss.logging.Logger;

/**
 * A build time assembled {@link FactoryFinderResolver}. Classes are loaded on first lookup so that service classes the
 * application never uses are neither loaded nor initialized at startup.
 */
public class FastFactoryFinderResolver extends DefaultFactoryFinderResolver {
    private static final Logger LOG = Logger.getLogger(FastFactoryFinderResolver.class);
    private final Map<String, String> classNames;
    private final Map<String, Class<?>> classMap = new ConcurrentHashMap<>();
    private final ClassLoader classLoader;

    FastFactoryFinderResolver(Map<String, String> classNames, ClassLoader classLoader) {
        this.classNames = classNames;
        this.classLoader = classLoader;
    }

    static String mapKey(String resourcePath, String key) {
        final int len = resourcePath.length() + key.length() + 1;
        final StringBuilder sb = new StringBuilder(len);

        if (resourcePath.startsWith("/")) {
            sb.append(resourcePath, 1, resourcePath.length());
        } else {
            sb.append(resourcePath);
        }

        if (!resourcePath.endsWith("/")) {
            sb.append("/");
        }

        sb.append(key);

        return sb.toString();
    }

    @Override
    public FactoryFinder resolveFactoryFinder(ClassResolver classResolver, String resourcePath) {
        return new FastFactoryFinder(resourcePath);
    }

    @Override
    public FactoryFinder resolveBootstrapFactoryFinder(ClassResolver classResolver, String resourcePath) {
        return new FastFactoryFinder(resourcePath);
    }

    // exposed for testing purpose
    public Map<String, Class<?>> getClassMap() {
        Map<String, Class<?>> result = new LinkedHashMap<>();
        classNames.keySet().forEach(key -> result.put(key, resolve(key)));
        return result;
    }

    private Class<?> resolve(String mapKey) {
        final String className = classNames.get(mapKey);
        if (className == null) {
            return null;
        }
        return classMap.computeIfAbsent(mapKey, k -> {
            try {
                return Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeCamelException("Could not load " + className + " for " + mapKey, e);
            }
        });
    }

    public static class Builder {
        private Map<String, String> classNames = new HashMap<>();

        public Builder entry(String resourcePath, String className) {
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            classNames.put(resourcePath, className);
            return this;
        }

        public FastFactoryFinderResolver build() {
            Map<String, String> cn = classNames;
            classNames = null; // make sure the classNames does not leak through re-using the builder

            if (LOG.isDebugEnabled()) {
                cn.forEach((key, value) -> LOG.debugf("FactoryFinder entry " + key + ": " + value));
            }

            return new FastFactoryFinderResolver(cn, Thread.currentThread().getContextClassLoader());
        }
    }

    public class FastFactoryFinder implements FactoryFinder {

        private final String path;

        FastFactoryFinder(String resourcePath) {
            this.path = resourcePath;
        }

        @Override
        public String getResourcePath() {
            return path;
        }

        @Override
        public Optional<Object> newInstance(String key) {
            return doNewInstance(key);
        }

        @Override
        public <T> Optional<T> newInstance(String key, Class<T> type) {
            return doNewInstance(key).map(type::cast);
        }

        @Override
        public Optional<Class<?>> findClass(String key) {
            final String mapKey = mapKey(path, key);
            final Class<?> cl = resolve(mapKey);
            LOG.tracef("Found a non-optional class for key %s: %s", mapKey, cl == null ? "null" : cl.getName());
            return Optional.ofNullable(cl);
        }

        @Override
        public Optional<Class<?>> findOptionalClass(String key) {
            final String mapKey = mapKey(path, key);
            final Class<?> cl = resolve(mapKey);
            LOG.tracef("Found an optional class for key %s: %s", mapKey, cl == null ? "null" : cl.getName());
            return Optional.ofNullable(cl);
        }

        @Override
        public void clear() {
            //nothing to clear
        }

        private Optional<Object> doNewInstance(String key) {
            return findClass(key).map(ObjectHelper::newInstance);
        }

    }

}
