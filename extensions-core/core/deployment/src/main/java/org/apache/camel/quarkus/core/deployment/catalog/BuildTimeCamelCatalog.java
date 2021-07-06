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
package org.apache.camel.quarkus.core.deployment.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.catalog.impl.AbstractCamelCatalog;
import org.apache.camel.tooling.model.BaseOptionModel;
import org.apache.camel.tooling.model.ComponentModel;

public class BuildTimeCamelCatalog extends AbstractCamelCatalog {

    public BuildTimeCamelCatalog(BuildTimeJsonSchemaResolver resolver) {
        setJSonSchemaResolver(resolver);
    }

    public BuildTimeJsonSchemaResolver getJSonSchemaResolver() {
        return (BuildTimeJsonSchemaResolver) super.getJSonSchemaResolver();
    }

    /**
     * Gets a list of all component, endpoint, dataformat & language options for the components that are on the classpath
     * 
     * @return List of {@link BaseOptionModel} instances
     */
    public List<BaseOptionModel> getAllOptions() {
        List<BaseOptionModel> options = new ArrayList<>();
        BuildTimeJsonSchemaResolver resolver = getJSonSchemaResolver();
        Set<SchemaResource> schemaResources = resolver.getSchemaResources();

        // Component options
        List<ComponentModel> componentModels = schemaResources
                .stream()
                .map(SchemaResource::getName)
                .map(this::componentModel)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // For components we need to combine the component options with the endpoint options
        componentModels.stream()
                .flatMap(componentModel -> componentModel.getComponentOptions().stream())
                .forEach(options::add);

        componentModels.stream()
                .flatMap(componentModel -> componentModel.getEndpointOptions().stream())
                .forEach(options::add);

        // DataFormat options
        schemaResources
                .stream()
                .map(SchemaResource::getName)
                .map(this::dataFormatModel)
                .filter(Objects::nonNull)
                .flatMap(dataFormatModel -> dataFormatModel.getOptions().stream())
                .forEach(options::add);

        // Language options
        schemaResources
                .stream()
                .map(SchemaResource::getName)
                .map(this::languageModel)
                .filter(Objects::nonNull)
                .flatMap(languageModel -> languageModel.getOptions().stream())
                .forEach(options::add);

        return options;
    }
}
