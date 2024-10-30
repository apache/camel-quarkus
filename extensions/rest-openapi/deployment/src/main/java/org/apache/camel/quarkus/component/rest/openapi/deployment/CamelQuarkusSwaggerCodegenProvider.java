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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.CodegenArgument;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.config.CodegenConfigurator;
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
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        final Config config = context.config();
        if (!config.getValue("quarkus.camel.openapi.codegen.enabled", Boolean.class)) {
            LOG.info("Skipping " + this.getClass() + " invocation on user's request");
            return false;
        }

        try {
            List<String> specFiles = new ArrayList<>();
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

            String packageName = config.getValue("quarkus.camel.openapi.codegen.model-package", String.class);
            String models = config.getOptionalValue("quarkus.camel.openapi.codegen.models", String.class).orElse("");
            boolean useBeanValidation = config.getValue("quarkus.camel.openapi.codegen.use-bean-validation", Boolean.class);
            boolean notNullJackson = config.getValue("quarkus.camel.openapi.codegen.not-null-jackson", Boolean.class);
            boolean ignoreUnknownProperties = config.getValue("quarkus.camel.openapi.codegen.ignore-unknown-properties",
                    Boolean.class);

            for (String specFile : specFiles) {
                CodegenConfigurator configurator = new CodegenConfigurator();
                configurator.setLang("quarkus");
                configurator.setLibrary("quarkus3");
                configurator.setModelPackage(packageName);
                configurator.setInputSpecURL(specFile);
                configurator.setOutputDir(context.outDir().toAbsolutePath().toString());
                System.setProperty(CodegenConstants.MODELS, models);
                configurator.getCodegenArguments()
                        .add(new CodegenArgument().option(CodegenConstants.API_DOCS_OPTION).type("boolean").value("false"));
                configurator.getCodegenArguments()
                        .add(new CodegenArgument().option(CodegenConstants.MODEL_DOCS_OPTION).type("boolean").value("false"));
                if (useBeanValidation) {
                    configurator.getAdditionalProperties().put(USE_BEANVALIDATION, true);
                }
                if (notNullJackson) {
                    configurator.getAdditionalProperties().put(NOT_NULL_JACKSON_ANNOTATION, true);
                }
                if (ignoreUnknownProperties) {
                    configurator.getAdditionalProperties().put("ignoreUnknownProperties", true);
                }
                config.getPropertyNames().forEach(name -> {
                    if (name.startsWith("quarkus.camel.openapi.codegen.additional-properties")) {
                        String key = name.substring("quarkus.camel.openapi.codegen.additional-properties.".length());
                        String value = config.getValue(name, String.class);
                        if (configurator.getAdditionalProperties().containsKey(key)) {
                            LOG.warn("Overriding existing property: " + key + " with value: " + value);
                        }
                        if (value.equals("true") || value.equals("false")) {
                            configurator.getAdditionalProperties().put(key, Boolean.parseBoolean(value));
                        } else {
                            configurator.getAdditionalProperties().put(key, value);
                        }
                    }
                });

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
