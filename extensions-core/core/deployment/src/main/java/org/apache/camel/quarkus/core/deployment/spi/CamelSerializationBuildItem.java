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

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Signal that the basic classes listed in
 * {@code org.apache.camel.quarkus.core.deployment.CamelSerializationProcessor.BASE_SERIALIZATION_CLASSES}, together
 * with any
 * additional classes passed to this build item, should be registered for serialization.
 * <p>
 * Registrations requested via this build item can be vetoed by the user with
 * {@code quarkus.camel.native.reflection.serialization-enabled=false}. Extensions that need the base set of classes
 * must
 * therefore route their serialization registrations through this build item, instead of producing
 * {@code ReflectiveClassBuildItem.serializationClass(..)} directly.
 */
public final class CamelSerializationBuildItem extends MultiBuildItem {
    private final List<String> classNames;

    public CamelSerializationBuildItem() {
        this.classNames = List.of();
    }

    public CamelSerializationBuildItem(String... classNames) {
        this.classNames = List.of(classNames);
    }

    /**
     * Additional classes to register for serialization, on top of the base set.
     */
    public List<String> getClassNames() {
        return classNames;
    }
}
