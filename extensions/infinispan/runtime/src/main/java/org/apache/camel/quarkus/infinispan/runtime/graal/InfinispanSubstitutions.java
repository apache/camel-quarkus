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
package org.apache.camel.quarkus.infinispan.runtime.graal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.component.infinispan.InfinispanManager;
import org.apache.camel.util.ObjectHelper;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.DefaultCacheManager;
import org.slf4j.Logger;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

class InfinispanSubstitutions {
}

@TargetClass(DefaultCacheManager.class)
final class Target_org_infinispan_manager_DefaultCacheManager {

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager() {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(Configuration defaultConfiguration) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(Configuration defaultConfiguration, boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(GlobalConfiguration globalConfiguration) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(GlobalConfiguration globalConfiguration, boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(GlobalConfiguration globalConfiguration,
            Configuration defaultConfiguration) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(GlobalConfiguration globalConfiguration,
            Configuration defaultConfiguration,
            boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(String configurationFile) throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(String configurationFile, boolean start) throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(InputStream configurationStream) throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(InputStream configurationStream, boolean start)
            throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(ConfigurationBuilderHolder holder, boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }
}

@TargetClass(InfinispanManager.class)
final class Target_org_apache_camel_component_infinispan_InfinispanManager {
    @Alias
    private BasicCacheContainer cacheContainer;
    @Alias
    private static transient Logger LOGGER;

    @Substitute
    public <K, V> BasicCache<K, V> getCache(String cacheName) {
        BasicCache<K, V> cache;
        if (ObjectHelper.isEmpty(cacheName)) {
            cache = cacheContainer.getCache();
            cacheName = cache.getName();
        } else {
            cache = cacheContainer.getCache(cacheName);
        }
        LOGGER.trace("Cache[{}]", cacheName);
        return cache;
    }
}
