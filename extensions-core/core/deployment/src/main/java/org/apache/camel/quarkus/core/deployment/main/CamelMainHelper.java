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
package org.apache.camel.quarkus.core.deployment.main;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultPackageScanResourceResolver;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.spi.Resource;
import org.apache.camel.util.AntPathMatcher;

public final class CamelMainHelper {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private CamelMainHelper() {
    }

    public static Stream<String> routesIncludePattern() {
        final String[] i1 = CamelSupport.getOptionalConfigValue(
                "camel.main.routes-include-pattern", String[].class, EMPTY_STRING_ARRAY);
        final String[] i2 = CamelSupport.getOptionalConfigValue(
                "camel.main.routesIncludePattern", String[].class, EMPTY_STRING_ARRAY);

        return i1.length == 0 && i2.length == 0
                ? Stream.empty()
                : Stream.concat(Stream.of(i1), Stream.of(i2)).filter(location -> !"false".equals(location));
    }

    public static Stream<String> routesExcludePattern() {
        final String[] i1 = CamelSupport.getOptionalConfigValue(
                "camel.main.routes-exclude-pattern", String[].class, EMPTY_STRING_ARRAY);
        final String[] i2 = CamelSupport.getOptionalConfigValue(
                "camel.main.routesExcludePattern", String[].class, EMPTY_STRING_ARRAY);

        return i1.length == 0 && i2.length == 0
                ? Stream.empty()
                : Stream.concat(Stream.of(i1), Stream.of(i2)).filter(location -> !"false".equals(location));
    }

    /**
     * Execute a task for each resource that matches with the "include" and "exclude" patterns.
     * 
     * @param resourceConsumer the task to execute for each matching resource.
     */
    public static void forEachMatchingResource(Consumer<Resource> resourceConsumer) throws Exception {
        try (DefaultPackageScanResourceResolver resolver = new DefaultPackageScanResourceResolver()) {
            resolver.setCamelContext(new DefaultCamelContext());
            String[] excludes = routesExcludePattern().toArray(String[]::new);
            for (String include : routesIncludePattern().collect(Collectors.toList())) {
                for (Resource resource : resolver.findResources(include)) {
                    if (AntPathMatcher.INSTANCE.anyMatch(excludes, resource.getLocation())) {
                        return;
                    }
                    resourceConsumer.accept(resource);
                }
            }
        }
    }
}
