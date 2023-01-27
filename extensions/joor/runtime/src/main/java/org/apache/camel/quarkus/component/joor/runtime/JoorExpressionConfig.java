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

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration options for the Camel jOOR language.
 */
@ConfigRoot(name = "camel.joor", phase = ConfigPhase.BUILD_TIME)
public class JoorExpressionConfig {

    /** Indicates whether a jOOR expression can use single quotes instead of double quotes. */
    @ConfigItem(defaultValue = "true")
    public boolean singleQuotes;

    /** The specific location of the configuration of the jOOR language. */
    @ConfigItem
    public Optional<String> configResource;

    /** The specific default result type of an expression expressed in jOOR language. */
    @ConfigItem
    public Optional<String> resultType;

    /** In JVM mode, indicates whether the expressions must be compiled at build time. */
    @ConfigItem(defaultValue = "false")
    public boolean compileAtBuildTime;
}
