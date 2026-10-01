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
package org.apache.camel.quarkus.component.cli.connector.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.Version;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import org.apache.camel.quarkus.component.cli.connector.CamelCliConnectorConfig;
import org.apache.camel.quarkus.component.cli.connector.CamelCliConnectorRecorder;
import org.apache.camel.quarkus.core.JvmOnlyRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.spi.CliConnectorFactory;
import org.jboss.logging.Logger;

@BuildSteps(onlyIf = CliConnectorProcessor.CliConnectorEnabled.class)
class CliConnectorProcessor {

    private static final Logger LOG = Logger.getLogger(CliConnectorProcessor.class);
    private static final String FEATURE = "camel-cli-connector";
    // a name, not the class: it needs quarkus-websockets-next, an optional dependency
    private static final String WEBSOCKET_CLIENT = "org.apache.camel.quarkus.component.cli.connector.QuarkusCliWebSocketClient";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    CamelBeanBuildItem camelBeanBuildItem(CamelCliConnectorRecorder recorder) {
        return new CamelBeanBuildItem("quarkusCliConnectorFactory",
                CliConnectorFactory.class.getName(),
                recorder.createCliConnectorFactory(Version.getVersion()));
    }

    /**
     * The WebSocket transport uses the WebSockets Next client when the application has it, the JDK client otherwise.
     */
    @BuildStep
    void webSocketClient(
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> configDefaults) {
        if (capabilities.isPresent(Capability.WEBSOCKETS_NEXT)) {
            // the back-pressure of the WebSockets Next client (since Quarkus 3.40) fetches one frame per message
            // received, so every message received in several frames (over 64 KB) leaves fewer frames to fetch, until
            // the connection stops reading: no back-pressure by default, as before Quarkus 3.40
            // TODO: Remove when https://github.com/quarkusio/quarkus/issues/57079 is fixed
            configDefaults.produce(new RunTimeConfigurationDefaultBuildItem(
                    "quarkus.websockets-next.client.max-pending-messages", "0"));
            additionalBeans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(WEBSOCKET_CLIENT)
                    .setDefaultScope(DotNames.SINGLETON)
                    .setUnremovable()
                    .build());
        }
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

    static class CliConnectorEnabled implements BooleanSupplier {
        CamelCliConnectorConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
