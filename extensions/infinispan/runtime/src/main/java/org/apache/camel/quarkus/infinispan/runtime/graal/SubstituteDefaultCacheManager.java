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

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.DefaultCacheManager;

@TargetClass(DefaultCacheManager.class)
final class SubstituteDefaultCacheManager {

    @Substitute
    public SubstituteDefaultCacheManager() {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(Configuration defaultConfiguration) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(Configuration defaultConfiguration, boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(GlobalConfiguration globalConfiguration) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(GlobalConfiguration globalConfiguration, boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(GlobalConfiguration globalConfiguration,
                                         Configuration defaultConfiguration) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(GlobalConfiguration globalConfiguration,
                                         Configuration defaultConfiguration,
                                         boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(String configurationFile) throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(String configurationFile, boolean start) throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(InputStream configurationStream) throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(InputStream configurationStream, boolean start)
            throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public SubstituteDefaultCacheManager(ConfigurationBuilderHolder holder, boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }
}
