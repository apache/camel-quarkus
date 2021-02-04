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
package org.apache.camel.quarkus.main.deployment;

import java.util.stream.Stream;

import org.apache.camel.quarkus.core.deployment.util.CamelSupport;

public final class CamelMainHelper {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private CamelMainHelper() {
    }

    public static Stream<String> routesIncludePatter() {
        final String[] i1 = CamelSupport.getOptionalConfigValue(
                "camel.main.routes-include-pattern", String[].class, EMPTY_STRING_ARRAY);
        final String[] i2 = CamelSupport.getOptionalConfigValue(
                "camel.main.routesIncludePattern", String[].class, EMPTY_STRING_ARRAY);

        return i1.length == 0 && i2.length == 0
                ? Stream.empty()
                : Stream.concat(Stream.of(i1), Stream.of(i2)).filter(location -> !"false".equals(location));
    }

    public static Stream<String> routesExcludePatter() {
        final String[] i1 = CamelSupport.getOptionalConfigValue(
                "camel.main.routes-exclude-pattern", String[].class, EMPTY_STRING_ARRAY);
        final String[] i2 = CamelSupport.getOptionalConfigValue(
                "camel.main.routesExcludePattern", String[].class, EMPTY_STRING_ARRAY);

        return i1.length == 0 && i2.length == 0
                ? Stream.empty()
                : Stream.concat(Stream.of(i1), Stream.of(i2)).filter(location -> !"false".equals(location));
    }
}
