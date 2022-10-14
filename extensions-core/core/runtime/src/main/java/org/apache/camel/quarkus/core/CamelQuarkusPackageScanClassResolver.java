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

import java.util.Objects;
import java.util.Set;

import org.apache.camel.impl.engine.DefaultPackageScanClassResolver;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.PackageScanFilter;

/**
 * Custom {@link PackageScanClassResolver} where classes / packages known to be scanned can be computed at build time
 * and cached for resolution at runtime. This is primarily needed for native mode where the default implementation
 * reliance on ClassLoader.getResources is likely to fail due to resources not being embedded within the native image.
 */
public class CamelQuarkusPackageScanClassResolver extends DefaultPackageScanClassResolver {
    private final Set<? extends Class<?>> classCache;

    public CamelQuarkusPackageScanClassResolver(Set<? extends Class<?>> classCache) {
        this.classCache = Objects.requireNonNull(classCache);
    }

    @Override
    protected void find(PackageScanFilter test, String packageName, ClassLoader loader, Set<Class<?>> classes) {
        classCache.stream()
                .filter(clazz -> clazz.getPackageName().replace('.', '/').equals(packageName))
                .filter(test::matches)
                .forEach(classes::add);

        // Try to fallback on default package scanning
        if (classes.isEmpty()) {
            super.find(test, packageName, loader, classes);
        }
    }
}
