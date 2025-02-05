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

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.camel.openapi")
public interface RestOpenApiBuildTimeConfig {
    /**
     * Build time configuration options for Camel Quarkus REST OpenAPI code generator.
     *
     * @asciidoclet
     */
    CodeGenConfig codegen();

    @ConfigGroup
    interface CodeGenConfig {
        /**
         * If `true`, Camel Quarkus OpenAPI code generation is run for .json and .yaml files discovered from the `openapi`
         * directory. When
         * `false`, code generation for .json and .yaml files is disabled.
         *
         * @asciidoclet
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * The package to use for generated model classes.
         *
         * @asciidoclet
         */
        @WithDefault("org.apache.camel.quarkus")
        String modelPackage();

        /**
         * A comma separated list of models to generate. The default is empty list for all models.
         *
         * @asciidoclet
         */
        Optional<String> models();

        /**
         * If `true`, use bean validation annotations in the generated model classes.
         *
         * @asciidoclet
         */
        @WithDefault("false")
        boolean useBeanValidation();

        /**
         * If `true`, use NON_NULL Jackson annotation in the generated model classes.
         *
         * @asciidoclet
         */
        @WithDefault("false")
        boolean notNullJackson();

        /**
         * If `true`, use JsonIgnoreProperties(ignoreUnknown = true) annotation in the generated model classes.
         *
         * @asciidoclet
         */
        @WithDefault("false")
        boolean ignoreUnknownProperties();

        /**
         * Additional properties to be used in the mustache templates.
         *
         * @asciidoclet
         */
        Map<String, String> additionalProperties();

        /**
         * A comma separated list of OpenAPI spec locations.
         *
         * @asciidoclet
         */
        Optional<String> locations();
    }
}
