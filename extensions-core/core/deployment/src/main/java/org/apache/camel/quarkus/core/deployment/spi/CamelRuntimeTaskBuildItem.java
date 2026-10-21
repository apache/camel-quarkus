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

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;

/**
 * Marker used as synchronization item for Build Steps that record {@link ExecutionTime#RUNTIME_INIT} tasks that must
 * complete before the {@link org.apache.camel.quarkus.core.CamelRuntime} is assembled and started.
 * <p>
 * As example some beans can be bound to the {@link org.apache.camel.spi.Registry} at {@link ExecutionTime#STATIC_INIT}
 * but others can only be bound at {@link ExecutionTime#RUNTIME_INIT}. Producing a CamelRuntimeTaskBuildItem from the
 * Build Step doing the binding ensures that it happens before the Camel runtime starts.
 */
public final class CamelRuntimeTaskBuildItem extends MultiBuildItem {
    private final String name;

    public CamelRuntimeTaskBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
