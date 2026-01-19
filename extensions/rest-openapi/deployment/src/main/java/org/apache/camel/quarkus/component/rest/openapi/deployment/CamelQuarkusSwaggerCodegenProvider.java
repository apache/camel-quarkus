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

package org.apache.camel.quarkus.component.rest.openapi.deployment;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.smallrye.config.SmallRyeConfig;
import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.CodegenArgument;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.config.CodegenConfigurator;
import org.apache.camel.quarkus.rest.openapi.runtime.RestOpenApiBuildTimeConfig;
import org.apache.camel.quarkus.rest.openapi.runtime.RestOpenApiBuildTimeConfig.CodeGenConfig;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import static io.swagger.codegen.v3.generators.features.BeanValidationFeatures.USE_BEANVALIDATION;
import static io.swagger.codegen.v3.generators.features.NotNullAnnotationFeatures.NOT_NULL_JACKSON_ANNOTATION;

public class CamelQuarkusSwaggerCodegenProvider implements CodeGenProvider {
    private static final Logger LOG = Logger.getLogger(CamelQuarkusSwaggerCodegenProvider.class);

    @Override
    public String providerId() {
        return "camel-quarkus-rest-openapi";
    }

    @Override
    public String[] inputExtensions() {
        return new String[] { "json", "yaml" };
    }

    @Override
    public String inputDirectory() {
        return "openapi";
    }

    @Override
    public boolean shouldRun(Path sourceDir, Config config) {
        return Files.isDirectory(sourceDir)
                || config.getOptionalValue("quarkus.camel.openapi.codegen.locations", String.class).isPresent();
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        final CodeGenConfig config = context.config()
                .unwrap(SmallRyeConfig.class)
                .getConfigMapping(RestOpenApiBuildTimeConfig.class)
                .codegen();

        if (!config.enabled()) {
            LOG.info("Skipping " + this.getClass() + " invocation on user's request");
            return false;
        }

        try {
            Set<String> specFiles = new HashSet<>();
            if (Files.isDirectory(context.inputDir())) {
                try (Stream<Path> protoFilesPaths = Files.walk(context.inputDir())) {
                    protoFilesPaths
                            .filter(Files::isRegularFile)
                            .filter(s -> s.toString().endsWith("json") || s.toString().endsWith("yaml"))
                            .map(Path::normalize)
                            .map(Path::toAbsolutePath)
                            .map(Path::toString)
                            .forEach(specFiles::add);
                }
            }

            config.locations().ifPresent(locations -> {
                for (String location : locations.split(",")) {
                    try {
                        URI uri;
                        if (location.indexOf("://") == -1) {
                            uri = Thread.currentThread().getContextClassLoader().getResource(location).toURI();
                        } else {
                            uri = new URI(location);
                        }
                        Path path = Path.of(uri);
                        specFiles.add(path.toAbsolutePath().toString());
                    } catch (Exception e) {
                        LOG.warnf(e, "Can not find location %s", location);
                    }
                }
            });

            for (String specFile : specFiles) {
                LOG.infof("Generating models for %s", specFile);
                CodegenConfigurator configurator = new CodegenConfigurator();
                configurator.setLang("quarkus");
                configurator.setLibrary("quarkus3");
                configurator.setModelPackage(config.modelPackage());
                configurator.setInputSpecURL(specFile);
                configurator.setOutputDir(context.outDir().toAbsolutePath().toString());
                System.setProperty(CodegenConstants.MODELS, config.models().orElse(""));
                configurator.getCodegenArguments()
                        .add(new CodegenArgument().option(CodegenConstants.API_DOCS_OPTION).type("boolean").value("false"));
                configurator.getCodegenArguments()
                        .add(new CodegenArgument().option(CodegenConstants.MODEL_DOCS_OPTION).type("boolean").value("false"));

                if (config.useBeanValidation()) {
                    configurator.getAdditionalProperties().put(USE_BEANVALIDATION, true);
                }

                if (config.notNullJackson()) {
                    configurator.getAdditionalProperties().put(NOT_NULL_JACKSON_ANNOTATION, true);
                }

                if (config.ignoreUnknownProperties()) {
                    configurator.getAdditionalProperties().put("ignoreUnknownProperties", true);
                }

                config.additionalProperties().forEach((key, value) -> {
                    if (configurator.getAdditionalProperties().containsKey(key)) {
                        LOG.warn("Overriding existing property: " + key + " with value: " + value);
                    }

                    if (value.equals("true") || value.equals("false")) {
                        configurator.getAdditionalProperties().put(key, Boolean.parseBoolean(value));
                    } else {
                        configurator.getAdditionalProperties().put(key, value);
                    }
                });

                configurator.setTypeMappings(config.typeMappings());

                final ClientOptInput input = configurator.toClientOptInput();
                new DefaultGenerator().opts(input).generate();
            }
            return true;
        } catch (IOException e) {
            throw new CodeGenException(
                    "Failed to generate java files from json file in " + context.inputDir().toAbsolutePath(), e);
        }
    }
}
