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

import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.ConfigProvider;

public final class CamelConfigFlags {
    private CamelConfigFlags() {
    }

    private static boolean asBoolean(String key, boolean defaultValue) {
        return ConfigProvider.getConfig().getOptionalValue(key, Boolean.class).orElse(defaultValue);
    }

    public static final class BootstrapEnabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return asBoolean("quarkus.camel.bootstrap.enabled", true);
        }
    }

    public static final class RoutesDiscoveryEnabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return asBoolean("quarkus.camel.routes-discovery.enabled", true);
        }
    }

    public static final class RuntimeCatalogEnabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return asBoolean("quarkus.camel.runtime-catalog.components", true)
                    || asBoolean("quarkus.camel.runtime-catalog.languages", true)
                    || asBoolean("quarkus.camel.runtime-catalog.dataformats", true)
                    || asBoolean("quarkus.camel.runtime-catalog.models", true);
        }
    }
}
