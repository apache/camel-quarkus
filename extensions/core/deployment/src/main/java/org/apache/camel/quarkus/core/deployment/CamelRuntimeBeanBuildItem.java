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

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.runtime.RuntimeValue;

/**
 * A {@link MultiBuildItem} holding beans to add to {@link org.apache.camel.spi.Registry} during
 * {@link ExecutionTime#RUNTIME_INIT} phase.
 * <p>
 * You should use the sibling {@link CamelBeanBuildItem} for all beans that can be produced during
 * {@link ExecutionTime#STATIC_INIT} phase.
 * <p>
 * Note that the field type should refer to the most specialized class to avoid the issue described in
 * https://issues.apache.org/jira/browse/CAMEL-13948.
 */
public final class CamelRuntimeBeanBuildItem extends MultiBuildItem implements CamelBeanInfo {
    private final String name;
    private final String type;
    private final RuntimeValue<?> value;

    public CamelRuntimeBeanBuildItem(String name, String type, RuntimeValue<?> value) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.value = Objects.requireNonNull(value);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public RuntimeValue<?> getValue() {
        return value;
    }
}
