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
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.quarkus.core.CamelRuntime;

/**
 * Holds the {@link CamelRuntime} {@link RuntimeValue}.
 *
 * @deprecated to access the runtime, inject the {@link CamelRuntime} bean. To run a
 *             {@link io.quarkus.deployment.annotations.ExecutionTime#RUNTIME_INIT} task before Camel starts, produce a
 *             {@link CamelRuntimeTaskBuildItem}. Deprecated in 3.40.1, will be removed in 4.0.0.
 */
@Deprecated(since = "3.40.1", forRemoval = true)
public final class CamelRuntimeBuildItem extends SimpleBuildItem {
    private final RuntimeValue<CamelRuntime> runtime;
    private final boolean autoStartup;

    public CamelRuntimeBuildItem(RuntimeValue<CamelRuntime> runtime, boolean autoStartup) {
        this.runtime = runtime;
        this.autoStartup = autoStartup;
    }

    public RuntimeValue<CamelRuntime> runtime() {
        return runtime;
    }

    public boolean isAutoStartup() {
        return autoStartup;
    }
}
