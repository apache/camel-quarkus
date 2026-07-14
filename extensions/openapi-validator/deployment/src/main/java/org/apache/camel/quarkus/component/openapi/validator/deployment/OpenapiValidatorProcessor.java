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
package org.apache.camel.quarkus.component.openapi.validator.deployment;

import com.atlassian.oai.validator.schema.SwaggerV20Library;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jackson.JsonNodeReader;
import com.github.fge.jsonschema.core.messages.JsonSchemaCoreMessageBundle;
import com.github.fge.jsonschema.core.messages.JsonSchemaSyntaxMessageBundle;
import com.github.fge.jsonschema.core.util.RegexECMA262Helper;
import com.github.fge.jsonschema.keyword.validator.AbstractKeywordValidator;
import com.github.fge.jsonschema.messages.JsonSchemaConfigurationBundle;
import com.github.fge.jsonschema.messages.JsonSchemaValidationBundle;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class OpenapiValidatorProcessor {

    private static final String FEATURE = "camel-openapi-validator";
    private static final DotName ABSTRACT_KEYWORD_VALIDATOR = DotName
            .createSimple(AbstractKeywordValidator.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("com.atlassian.oai", "swagger-request-validator-core"));
        indexDependency.produce(new IndexDependencyBuildItem("com.github.java-json-tools", "json-schema-validator"));
        indexDependency.produce(new IndexDependencyBuildItem("com.github.java-json-tools", "json-schema-core"));
    }

    @BuildStep
    void registerReflectiveClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        // Keyword validators are instantiated via ReflectionKeywordValidatorFactory using Constructor.newInstance()
        String[] keywordValidators = combinedIndex.getIndex()
                .getAllKnownSubclasses(ABSTRACT_KEYWORD_VALIDATOR)
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(keywordValidators).constructors(true).build());

        // Message bundles are instantiated via MessageBundles.getBundle() using Constructor.newInstance()
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                SwaggerV20Library.ValidationBundle.class.getName(),
                SwaggerV20Library.SyntaxBundle.class.getName(),
                JsonSchemaCoreMessageBundle.class.getName(),
                JsonSchemaSyntaxMessageBundle.class.getName(),
                JsonSchemaConfigurationBundle.class.getName(),
                JsonSchemaValidationBundle.class.getName()).constructors(true).build());
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses
                .produce(new RuntimeInitializedClassBuildItem(JsonNodeReader.class.getName()));
        runtimeInitializedClasses
                .produce(new RuntimeInitializedClassBuildItem(JsonLoader.class.getName()));
        runtimeInitializedClasses
                .produce(new RuntimeInitializedClassBuildItem(RegexECMA262Helper.class.getName()));
    }

    @BuildStep
    void registerNativeImageResources(
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<NativeImageResourceDirectoryBuildItem> nativeImageResourceDirectories) {
        nativeImageResources.produce(new NativeImageResourceBuildItem(
                "swagger/validation/default-levels.properties",
                "swagger/validation/schema-validation.properties",
                "swagger/validation/messages.properties",
                "com/github/fge/jsonschema/validator/validation.properties",
                "com/github/fge/jsonschema/validator/configuration.properties",
                "com/github/fge/jsonschema/core/core.properties",
                "com/github/fge/jsonschema/core/syntax.properties",
                "com/github/fge/jackson/jsonNodeReader.properties",
                "com/github/fge/jackson/jsonpointer.properties",
                "com/github/fge/uritemplate/messages.properties"));
        // Register entire draft schema directories to capture all schema files (schema, hyper-schema, links, etc.)
        nativeImageResourceDirectories.produce(new NativeImageResourceDirectoryBuildItem("draftv3"));
        nativeImageResourceDirectories.produce(new NativeImageResourceDirectoryBuildItem("draftv4"));
    }
}
