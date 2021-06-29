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
package org.apache.camel.quarkus.component.opentelemetry;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.opentelemetry", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class CamelOpenTelemetryConfig {

    /**
     * Sets whether header keys need to be encoded
     */
    @ConfigItem(defaultValue = "false")
    public boolean encoding;

    /**
     * Sets whether to disable tracing for endpoint URIs that match the given patterns. The pattern can take the following
     * forms:
     * <p>
     * <p>
     * 1. An exact match on the endpoint URI. E.g platform-http:/some/path
     * <p>
     * <p>
     * 2. A wildcard match. E.g platform-http:*
     * <p>
     * <p>
     * 3. A regular expression matching the endpoint URI. E.g platform-http:/prefix/.*
     */
    @ConfigItem
    public Optional<List<String>> excludePatterns;
}

/*
 * <li>exact match, returns true</li>
 * <li>wildcard match (pattern ends with a * and the uri starts with the pattern), returns true</li>
 * <li>regular expression match, returns true</li>
 * <li>exact match with uri normalization of the pattern if possible, returns true</li>
 */
