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
package org.apache.camel.quarkus.component.caffeine.it;

import java.util.Locale;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
public class CaffeineCaches {
    public static final String SHARED_CACHE_NAME = "shared-cache";
    public static final String LOADING_CACHE_NAME = "loading-cache";

    @Named(SHARED_CACHE_NAME)
    static Cache<String, String> shared() {
        return Caffeine.newBuilder().build();
    }

    @Named(LOADING_CACHE_NAME)
    static LoadingCache<String, String> loading() {
        return Caffeine.newBuilder().build(new ToUpper());
    }

    public static class ToUpper implements CacheLoader<String, String> {
        @Override
        public String load(String key) throws Exception {
            return key.toUpperCase(Locale.US);
        }
    }
}
