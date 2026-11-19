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
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.camel.catalog.impl.AbstractCamelCatalog;
import org.apache.camel.tooling.model.BaseOptionModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.LanguageModel;

public class BuildTimeCamelCatalog extends AbstractCamelCatalog {

    public BuildTimeCamelCatalog(BuildTimeJsonSchemaResolver resolver) {
        setJSonSchemaResolver(resolver);
    }

    public BuildTimeJsonSchemaResolver getJSonSchemaResolver() {
        return (BuildTimeJsonSchemaResolver) super.getJSonSchemaResolver();
    }

    /**
     * Gets the models of the components that are on the classpath, one per component scheme
     *
     * @return List of {@link ComponentModel} instances
     */
    public List<ComponentModel> getComponentModels() {
        return getModels("component", this::componentModel);
    }

    /**
     * Gets the models of the data formats that are on the classpath
     *
     * @return List of {@link DataFormatModel} instances
     */
    public List<DataFormatModel> getDataFormatModels() {
        return getModels("dataformat", this::dataFormatModel);
    }

    /**
     * Gets the models of the languages that are on the classpath
     *
     * @return List of {@link LanguageModel} instances
     */
    public List<LanguageModel> getLanguageModels() {
        return getModels("language", this::languageModel);
    }

    /**
     * Gets a list of all component, endpoint, dataformat & language options for the components that are on the classpath
     *
     * @return List of {@link BaseOptionModel} instances
     */
    public List<BaseOptionModel> getAllOptions() {
        List<BaseOptionModel> options = new ArrayList<>();
        List<ComponentModel> componentModels = getComponentModels();
        componentModels.forEach(model -> options.addAll(model.getComponentOptions()));
        componentModels.forEach(model -> options.addAll(model.getEndpointOptions()));
        getDataFormatModels().forEach(model -> options.addAll(model.getOptions()));
        getLanguageModels().forEach(model -> options.addAll(model.getOptions()));
        return options;
    }

    /**
     * Gets the names of the component options of the given Java type, keyed by component scheme. Schemes without such
     * an option are omitted.
     *
     * @param  javaType the Java type of the options to find
     * @return          Map of component scheme to option names
     */
    public Map<String, List<String>> getComponentOptionNames(Class<?> javaType) {
        return getOptionNames(javaType, ComponentModel::getComponentOptions);
    }

    /**
     * Gets the names of the endpoint options of the given Java type, keyed by component scheme. Schemes without such
     * an option are omitted.
     *
     * @param  javaType the Java type of the options to find
     * @return          Map of component scheme to option names
     */
    public Map<String, List<String>> getEndpointOptionNames(Class<?> javaType) {
        return getOptionNames(javaType, ComponentModel::getEndpointOptions);
    }

    private Map<String, List<String>> getOptionNames(Class<?> javaType,
            Function<ComponentModel, List<? extends BaseOptionModel>> optionsResolver) {
        Map<String, List<String>> optionNames = new TreeMap<>();
        for (ComponentModel model : getComponentModels()) {
            List<String> names = optionsResolver.apply(model)
                    .stream()
                    .filter(option -> javaType.getName().equals(option.getJavaType()))
                    .map(BaseOptionModel::getName)
                    .collect(Collectors.toCollection(ArrayList::new));
            if (!names.isEmpty()) {
                optionNames.put(model.getScheme(), names);
            }
        }
        return optionNames;
    }

    private <T> List<T> getModels(String type, Function<String, T> modelResolver) {
        return getJSonSchemaResolver().getSchemaResources()
                .stream()
                .filter(resource -> type.equals(resource.getType()))
                .map(SchemaResource::getName)
                .map(modelResolver)
                .filter(Objects::nonNull)
                .toList();
    }
}
