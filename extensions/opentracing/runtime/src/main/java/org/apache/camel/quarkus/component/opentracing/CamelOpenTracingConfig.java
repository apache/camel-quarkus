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
package org.apache.camel.quarkus.component.opentracing;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.opentracing", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class CamelOpenTracingConfig {

    /**
     * Sets whether header names need to be encoded. Can be useful in situations where OpenTracing propagators
     * potentially set header name values in formats that are not compatible with the target system. E.g for JMS where the
     * specification mandates header names are valid Java identifiers.
     */
    @ConfigItem(defaultValue = "false")
    public boolean encoding;

    /**
     * Sets whether to disable tracing for endpoint URIs that match the given patterns
     */
    @ConfigItem
    public Optional<List<String>> excludePatterns;
}
