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
package org.apache.camel.quarkus.core;

import org.apache.camel.CamelContext;
import org.apache.camel.runtimecatalog.impl.DefaultRuntimeCamelCatalog;

public class CamelRuntimeCatalog extends DefaultRuntimeCamelCatalog {
    private final CamelConfig.RuntimeCatalogConfig config;

    public CamelRuntimeCatalog(CamelContext camelContext, CamelConfig.RuntimeCatalogConfig config) {
        super(camelContext, true);

        this.config = config;
    }

    @Override
    public String modelJSonSchema(String name) {
        if (!config.models) {
            return null;
        }

        return super.modelJSonSchema(name);
    }

    @Override
    public String componentJSonSchema(String name) {
        if (!config.components) {
            return null;
        }

        return super.componentJSonSchema(name);
    }

    @Override
    public String dataFormatJSonSchema(String name) {
        if (!config.dataformats) {
            return null;
        }

        return super.dataFormatJSonSchema(name);
    }

    @Override
    public String languageJSonSchema(String name) {
        if (!config.languages) {
            return null;
        }

        return super.languageJSonSchema(name);
    }
}
