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
package org.apache.camel.quarkus.core.deployment.spi;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.spi.Registry;

/**
 * Holds the {@link Registry} {@link RuntimeValue}. It is made available after the beans from
 * {@link CamelBeanBuildItem}s were registered in the underlying {@link Registry}.
 *
 * <h3>{@link CamelRuntimeRegistryBuildItem} vs. {@link CamelRegistryBuildItem}</h3>
 *
 * They both refer to the same instance of {@link Registry} but in a different phase of the application bootstrap:
 * {@link CamelRuntimeBeanBuildItem} is bound to {@link ExecutionTime#RUNTIME_INIT} phase while
 * {@link CamelRegistryBuildItem} is bound to {@link ExecutionTime#STATIC_INIT}
 * phase.
 *
 */
public final class CamelRegistryBuildItem extends SimpleBuildItem {
    private final RuntimeValue<Registry> value;

    public CamelRegistryBuildItem(RuntimeValue<Registry> value) {
        this.value = value;
    }

    public RuntimeValue<Registry> getRegistry() {
        return value;
    }
}
