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
package org.apache.camel.quarkus.component.mcp.server.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import org.apache.camel.quarkus.component.mcp.server.CamelMcpServerConfig;
import org.apache.camel.quarkus.component.mcp.server.CamelMcpServerRecorder;
import org.apache.camel.quarkus.core.JvmOnlyRecorder;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;
import org.jboss.logging.Logger;

class McpServerProcessor {

    private static final Logger LOG = Logger.getLogger(McpServerProcessor.class);
    private static final String FEATURE = "camel-mcp-server";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = McpServerEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    RuntimeCamelContextCustomizerBuildItem mcpServerBridge(CamelMcpServerRecorder recorder, CamelMcpServerConfig config) {
        return new RuntimeCamelContextCustomizerBuildItem(
                recorder.createContextCustomizer(config.tags().orElse(null), config.toolTimeout()));
    }

    /**
     * Remove this once this extension starts supporting the native mode.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void warnJvmInNative(JvmOnlyRecorder recorder) {
        JvmOnlyRecorder.warnJvmInNative(LOG, FEATURE); // warn at build time
        recorder.warnJvmInNative(FEATURE); // warn at runtime
    }

    static final class McpServerEnabled implements BooleanSupplier {
        CamelMcpServerConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
