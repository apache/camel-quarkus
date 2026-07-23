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
package org.apache.camel.quarkus.component.micrometer.observability;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.camel.micrometer-observability")
public interface CamelMicrometerObservabilityConfig {
    /**
     * Sets whether to disable tracing for endpoint URIs or Processor ids that match the given comma separated patterns.
     * The pattern can take the following forms:
     *
     * 1. An exact match on the endpoint URI, e.g., {@code platform-http:/some/path}
     * 2. A wildcard match, e.g., {@code platform-http:*}
     * 3. A regular expression matching the endpoint URI, e.g., {@code platform-http:/prefix/.*}
     *
     * @asciidoclet
     */
    Optional<String> excludePatterns();

    /**
     * Sets include pattern(s) that will explicitly enable tracing for Camel processors that match the pattern.
     * Multiple patterns can be separated by comma. All processors are included by default if nothing is specified.
     *
     * @asciidoclet
     */
    Optional<String> includePatterns();

    /**
     * Sets whether to create new spans for each Camel Processor. Use the {@code excludePatterns} property to filter
     * out specific processors.
     *
     * When enabled, this generates much more detailed traces but also increases overhead.
     *
     * @asciidoclet
     */
    @WithDefault("false")
    boolean traceProcessors();

    /**
     * Disable tracing of inner core processors (any core DSL processor provided in the route, for example
     * {@code bean}, {@code log}, ...).
     *
     * @asciidoclet
     */
    @WithDefault("false")
    boolean disableCoreProcessors();

    /**
     * If set to {@code true}, adds the generated telemetry {@code CAMEL_TRACE_ID} and {@code CAMEL_SPAN_ID}
     * Exchange headers.
     *
     * @asciidoclet
     */
    @WithDefault("false")
    boolean traceHeadersInclusion();
}
