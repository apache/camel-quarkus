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

import java.io.File;
import java.util.Map;

import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.SupportingFile;
import io.swagger.codegen.v3.generators.features.BeanValidationFeatures;
import io.swagger.codegen.v3.generators.features.NotNullAnnotationFeatures;
import io.swagger.codegen.v3.generators.java.AbstractJavaCodegen;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.BooleanUtils;

import static io.swagger.codegen.v3.CodegenConstants.IS_ENUM_EXT_NAME;
import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

public class QuarkusCodegen extends AbstractJavaCodegen implements BeanValidationFeatures, NotNullAnnotationFeatures {
    protected boolean useBeanValidation = false;
    private boolean notNullJacksonAnnotation = false;

    public QuarkusCodegen() {
        super();
        importMapping.put("QuarkusRegisterForReflection", "io.quarkus.runtime.annotations.RegisterForReflection");
        importMapping.put("JsonIgnoreProperties", "com.fasterxml.jackson.annotation.JsonIgnoreProperties");
        supportedLibraries.put("quarkus3", "Quarkus 3 framework");
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "quarkus";
    }

    @Override
    public String getHelp() {
        return "Generate the models based on Quarkus framework";
    }

    @Override
    public String getDefaultTemplateDir() {
        return "Quarkus";
    }

    @Override
    public CodegenModel fromModel(String name, Schema schema, Map<String, Schema> allSchemas) {
        CodegenModel model = super.fromModel(name, schema, allSchemas);
        if (schema != null && "array".equals(schema.getType())) {
            additionalProperties.put("useQuarkusRegisterForReflection", false);
        }
        if (additionalProperties.containsKey("ignoreUnknownProperties")) {
            model.imports.add("JsonIgnoreProperties");
        }
        return model;
    }

    @Override
    public void processOpts() {
        if ("quarkus3".equals(library)) {
            dateLibrary = "java8";
            additionalProperties.put(JAKARTA, true);
            additionalProperties.put("jackson", "true");
        }

        super.processOpts();
        modelDocTemplateFiles.remove("model_doc.mustache");
        apiDocTemplateFiles.remove("api_doc.mustache");

        if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
            this.setUseBeanValidation(convertPropertyToBooleanAndWriteBack(USE_BEANVALIDATION));
        }
        final String invokerFolder = (sourceFolder + File.separator + invokerPackage).replace(".", File.separator);
        if (additionalProperties.containsKey("jackson")) {
            supportingFiles.add(new SupportingFile("RFC3339DateFormat.mustache", invokerFolder, "RFC3339DateFormat.java"));
        }

    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);
        boolean isEnum = getBooleanValue(model, IS_ENUM_EXT_NAME);
        if (!BooleanUtils.toBoolean(isEnum)) {
            //Needed imports for Jackson based libraries
            if (additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonProperty");
                model.imports.add("JsonValue");
            }
        } else { // enum class
            //Needed imports for Jackson's JsonCreator
            if (additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonValue");
                model.imports.add("JsonCreator");
            }
        }
        model.imports.add("QuarkusRegisterForReflection");
        additionalProperties.put("useQuarkusRegisterForReflection", true);
        if (additionalProperties.containsKey("ignoreUnknownProperties")) {
            model.imports.add("JsonIgnoreProperties");
        }
    }

    @Override
    public void setUseBeanValidation(boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }

    @Override
    public void setNotNullJacksonAnnotation(boolean notNullJacksonAnnotation) {
        this.notNullJacksonAnnotation = notNullJacksonAnnotation;
    }

    @Override
    public boolean isNotNullJacksonAnnotation() {
        return notNullJacksonAnnotation;
    }

    @Override
    public String modelFileFolder() {
        // Override the original impl to avoid redundant src/main/java directory prefix being added to the output path
        return outputFolder + "/" + modelPackage().replace('.', '/');
    }
}
