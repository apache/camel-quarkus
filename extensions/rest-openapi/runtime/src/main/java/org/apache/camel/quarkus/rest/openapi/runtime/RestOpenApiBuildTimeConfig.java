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
package org.apache.camel.quarkus.rest.openapi.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.openapi", phase = ConfigPhase.BUILD_TIME)
public class RestOpenApiBuildTimeConfig {

    /**
     * Build time configuration options for Camel Quarkus REST OpenAPI code generator.
     *
     * @asciidoclet
     */
    @ConfigItem
    public CodeGenConfig codegen;

    @ConfigGroup
    public static class CodeGenConfig {

        /**
         * If `true`, Camel Quarkus OpenAPI code generation is run for .json files discovered from the `openapi` directory. When
         * `false`, code generation for .json files is disabled.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;

        /**
         * The package to use for generated model classes.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = "org.apache.camel.quarkus")
        public String modelPackage;

        /**
         * A comma separated list of models to generate. All models is the default.
         *
         * @asciidoclet
         */
        @ConfigItem
        public Optional<String> models;

        /**
         * If `true`, use bean validation annotations in the generated model classes.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = "false")
        public boolean useBeanValidation;

        /**
         * If `true`, use NON_NULL Jackson annotation in the generated model classes.
         *
         * @asciidoclet
         */
        @ConfigItem(defaultValue = "false")
        public boolean notNullJackson;
    }
}
