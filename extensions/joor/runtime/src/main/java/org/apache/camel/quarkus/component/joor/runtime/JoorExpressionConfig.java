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
package org.apache.camel.quarkus.component.joor.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build time configuration options for the Camel jOOR language.
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.camel.joor")
public interface JoorExpressionConfig {
    /**
     * Indicates whether a jOOR expression can use single quotes instead of double quotes.
     *
     * @asciidoclet
     */
    @WithDefault("true")
    boolean singleQuotes();

    /**
     * The specific location of the configuration of the jOOR language.
     *
     * @asciidoclet
     */
    Optional<String> configResource();

    /**
     * In JVM mode, indicates whether the expressions must be compiled at build time.
     *
     * @asciidoclet
     */
    @WithDefault("false")
    boolean compileAtBuildTime();
}
