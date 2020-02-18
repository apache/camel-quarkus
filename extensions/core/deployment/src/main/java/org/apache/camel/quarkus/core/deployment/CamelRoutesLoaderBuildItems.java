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
package org.apache.camel.quarkus.core.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.quarkus.core.RegistryRoutesLoader;
import org.apache.camel.spi.XMLRoutesDefinitionLoader;

public final class CamelRoutesLoaderBuildItems {
    private CamelRoutesLoaderBuildItems() {
    }

    /**
     * Holds the {@link RegistryRoutesLoader} instance.
     */
    public static final class Registry extends SimpleBuildItem {
        private final RuntimeValue<RegistryRoutesLoader> value;

        public Registry(RuntimeValue<RegistryRoutesLoader> value) {
            this.value = value;
        }

        public RuntimeValue<RegistryRoutesLoader> getLoader() {
            return value;
        }
    }

    /**
     * Holds the {@link XMLRoutesDefinitionLoader} instance.
     */
    public static final class Xml extends SimpleBuildItem {
        private final RuntimeValue<XMLRoutesDefinitionLoader> value;

        public Xml(RuntimeValue<XMLRoutesDefinitionLoader> value) {
            this.value = value;
        }

        public RuntimeValue<XMLRoutesDefinitionLoader> getLoader() {
            return value;
        }
    }
}
