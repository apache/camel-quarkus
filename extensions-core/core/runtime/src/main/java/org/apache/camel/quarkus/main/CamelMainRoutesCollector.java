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
package org.apache.camel.quarkus.main;

import java.util.List;
import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.main.DefaultRoutesCollector;
import org.apache.camel.quarkus.core.RegistryRoutesLoader;

public class CamelMainRoutesCollector extends DefaultRoutesCollector {
    private final RegistryRoutesLoader registryRoutesLoader;
    private final Optional<List<String>> excludePatterns;
    private final Optional<List<String>> includePatterns;

    public CamelMainRoutesCollector(
            RegistryRoutesLoader registryRoutesLoader,
            Optional<List<String>> excludePatterns,
            Optional<List<String>> includePatterns) {
        this.registryRoutesLoader = registryRoutesLoader;
        this.excludePatterns = excludePatterns;
        this.includePatterns = includePatterns;
    }

    public RegistryRoutesLoader getRegistryRoutesLoader() {
        return registryRoutesLoader;
    }

    @Override
    public List<RoutesBuilder> collectRoutesFromRegistry(
            CamelContext camelContext,
            String excludePattern,
            String includePattern) {

        /**
         * The incoming excludePattern & includePattern are ignored since they are provided from camel-main via:
         *
         * camel.main.javaRoutesExcludePattern
         * camel.main.javaRoutesIncludePattern
         *
         * The values for those properties are combined with the quarkus.camel.routes-discovery equivalents at build time.
         */
        return registryRoutesLoader.collectRoutesFromRegistry(
                camelContext,
                getPatternString(excludePatterns),
                getPatternString(includePatterns));
    }

    private String getPatternString(Optional<List<String>> camelQuarkusPatterns) {
        return camelQuarkusPatterns.map(patterns -> String.join(",", patterns)).orElse(null);
    }
}
