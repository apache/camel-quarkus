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

import java.util.Optional;
import java.util.Set;

import org.apache.camel.catalog.JSonSchemaResolver;

public class BuildTimeJsonSchemaResolver implements JSonSchemaResolver {

    private final Set<SchemaResource> schemaResources;

    public BuildTimeJsonSchemaResolver(Set<SchemaResource> schemaResources) {
        this.schemaResources = schemaResources;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        throw new UnsupportedOperationException("Setting an alternative ClassLoader is not supported");
    }

    @Override
    public String getComponentJSonSchema(String name) {
        return resolveJsonSchema("component", name);
    }

    @Override
    public String getDataFormatJSonSchema(String name) {
        return resolveJsonSchema("dataformat", name);
    }

    @Override
    public String getLanguageJSonSchema(String name) {
        return resolveJsonSchema("language", name);
    }

    @Override
    public String getOtherJSonSchema(String name) {
        throw new UnsupportedOperationException("Other JSON schema resolution is not supported");
    }

    @Override
    public String getModelJSonSchema(String name) {
        throw new UnsupportedOperationException("Model JSON schema resolution is not supported");
    }

    @Override
    public String getMainJsonSchema() {
        throw new UnsupportedOperationException("Main JSON schema resolution is not supported");
    }

    public Set<SchemaResource> getSchemaResources() {
        return schemaResources;
    }

    private String resolveJsonSchema(String type, String name) {
        Optional<SchemaResource> schemaResource = schemaResources.stream()
                .filter(si -> si.getType().equals(type))
                .filter(si -> si.getName().equals(name))
                .findFirst();

        if (schemaResource.isPresent()) {
            return schemaResource.get().load();
        }

        return null;
    }
}
