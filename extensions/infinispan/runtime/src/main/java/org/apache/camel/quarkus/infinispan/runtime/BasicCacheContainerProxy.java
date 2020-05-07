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
package org.apache.camel.quarkus.infinispan.runtime;

import java.util.Set;

import org.apache.camel.quarkus.core.LazyProxy;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;

public class BasicCacheContainerProxy extends LazyProxy<BasicCacheContainer> implements BasicCacheContainer {

    public BasicCacheContainerProxy() {
    }

    public BasicCacheContainer getDelegate() {
        return super.getDelegate();
    }

    @Override
    public <K, V> BasicCache<K, V> getCache() {
        return getDelegate().getCache();
    }

    @Override
    public <K, V> BasicCache<K, V> getCache(String s) {
        return getDelegate().getCache(s);
    }

    @Override
    public Set<String> getCacheNames() {
        return getDelegate().getCacheNames();
    }

    @Override
    public void start() {
        getDelegate().start();
    }

    @Override
    public void stop() {
        getDelegate().stop();
    }
}
