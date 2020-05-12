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

import java.util.Objects;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.quarkus.core.deployment.CamelBeanInfo;

/**
 * A {@link MultiBuildItem} holding beans to add to {@link org.apache.camel.spi.Registry} during
 * {@link ExecutionTime#STATIC_INIT} phase.
 * <p>
 * You can use the sibling {@link CamelRuntimeBeanBuildItem} to register beans in the {@link ExecutionTime#RUNTIME_INIT}
 * phase - i.e. those ones that cannot be produced during {@link ExecutionTime#STATIC_INIT} phase.
 * <p>
 * Note that the field type should refer to the most specialized class to avoid the issue described in
 * https://issues.apache.org/jira/browse/CAMEL-13948.
 */
public final class CamelBeanBuildItem extends MultiBuildItem implements CamelBeanInfo {
    private final String name;
    private final String type;
    private final RuntimeValue<?> value;

    /**
     * @param name the name of the bean
     * @param type the Java type of the bean
     */
    public CamelBeanBuildItem(String name, String type) {
        this(name, type, null);
    }

    /**
     * @param name  the name of the bean
     * @param type  the Java type of the bean
     * @param value the value to be bound to the registry, if <code>null</code> a new instance will be create
     *              by the {@link org.apache.camel.quarkus.core.CamelMainRecorder}
     */
    public CamelBeanBuildItem(String name, String type, RuntimeValue<?> value) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    public Optional<RuntimeValue<?>> getValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CamelBeanInfo)) {
            return false;
        }
        CamelBeanInfo info = (CamelBeanInfo) o;
        return Objects.equals(getName(), info.getName()) &&
                Objects.equals(getType(), info.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getType());
    }
}
