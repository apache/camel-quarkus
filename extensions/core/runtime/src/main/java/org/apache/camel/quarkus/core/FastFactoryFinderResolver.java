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
import java.util.Map;
import java.util.Optional;

import org.apache.camel.impl.engine.DefaultFactoryFinderResolver;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.spi.FactoryFinder;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.support.ObjectHelper;
import org.jboss.logging.Logger;

/**
 * A build time assembled {@link FactoryFinderResolver}.
 */
public class FastFactoryFinderResolver extends DefaultFactoryFinderResolver {
    private static final Logger LOG = Logger.getLogger(FastFactoryFinderResolver.class);
    private final Map<String, Class<?>> classMap;

    FastFactoryFinderResolver(Map<String, Class<?>> classMap) {
        this.classMap = classMap;
    }

    @Override
    public FactoryFinder resolveFactoryFinder(ClassResolver classResolver, String resourcePath) {
        return new FastFactoryFinder(resourcePath);
    }

    static String mapKey(String resourcePath, String prefix, String key) {
        final int len = resourcePath.length() + (prefix == null ? 0 : prefix.length()) + key.length() + 1;
        final StringBuilder sb = new StringBuilder(len);
        if (resourcePath.startsWith("/")) {
            sb.append(resourcePath, 1, resourcePath.length());
        } else {
            sb.append(resourcePath);
        }
        if (!resourcePath.endsWith("/")) {
            sb.append("/");
        }
        if (prefix != null) {
            sb.append(prefix);
        }
        sb.append(key);
        return sb.toString();
    }

    public static class Builder {
        private Map<String, Class<?>> classMap = new HashMap<>();

        public Builder entry(String resourcePath, Class<?> cl) {
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            classMap.put(resourcePath, cl);
            return this;
        }

        public FastFactoryFinderResolver build() {
            Map<String, Class<?>> cm = classMap;
            classMap = null; // make sure the classMap does not leak through re-using the builder

            if (LOG.isDebugEnabled()) {
                cm.entrySet().forEach(
                        e -> LOG.debugf("FactoryFinder entry " + e.getKey() + ": " + e.getValue().getName()));
            }

            return new FastFactoryFinderResolver(cm);
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
            return Optional.ofNullable(doNewInstance(key, null));
        }

        @Override
        public <T> Optional<T> newInstance(String key, Class<T> type) {
            Object obj = doNewInstance(key, null);
            return Optional.ofNullable(type.cast(obj));
        }

        @Override
        public Optional<Class<?>> findClass(String key) {
            return findClass(key, null);
        }

        @Override
        public Optional<Class<?>> findClass(String key, String propertyPrefix) {
            final String mapKey = mapKey(path, propertyPrefix, key);
            final Class<?> cl = classMap.get(mapKey);
            LOG.tracef("Found a non-optional class for key %s: %s", mapKey, cl == null ? "null" : cl.getName());
            return Optional.ofNullable(cl);
        }

        @Override
        public Optional<Class<?>> findClass(String key, String propertyPrefix, Class<?> clazz) {
            // Just ignore clazz which is only useful for OSGiFactoryFinder
            return findClass(key, propertyPrefix);
        }

        @Override
        public Optional<Class<?>> findOptionalClass(String key, String propertyPrefix) {
            final String mapKey = mapKey(path, propertyPrefix, key);
            final Class<?> cl = classMap.get(mapKey);
            LOG.tracef("Found an optional class for key %s: %s", mapKey, cl == null ? "null" : cl.getName());
            return Optional.ofNullable(cl);
        }

        private Object doNewInstance(String key, String propertyPrefix) {
            Optional<Class<?>> clazz = findClass(key, propertyPrefix);
            return clazz.map(ObjectHelper::newInstance).orElse(null);
        }

    }

}
