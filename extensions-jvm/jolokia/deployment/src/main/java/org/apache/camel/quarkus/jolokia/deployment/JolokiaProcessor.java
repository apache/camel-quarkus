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
package org.apache.camel.quarkus.jolokia.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.quarkus.core.JvmOnlyRecorder;
import org.apache.camel.quarkus.jolokia.CamelQuarkusJolokiaServer;
import org.apache.camel.quarkus.jolokia.JolokiaRecorder;
import org.apache.camel.quarkus.jolokia.config.JolokiaBuildTimeConfig;
import org.apache.camel.quarkus.jolokia.config.JolokiaRuntimeConfig;
import org.jboss.logging.Logger;

@BuildSteps(onlyIf = JolokiaProcessor.JolokiaEnabled.class)
public class JolokiaProcessor {
    private static final Logger LOG = Logger.getLogger(JolokiaProcessor.class);
    private static final String FEATURE = "camel-jolokia";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    JolokiaServerConfigBuildItem createJolokiaServerConfig(
            ApplicationInfoBuildItem applicationInfo,
            JolokiaBuildTimeConfig buildTimeConfig,
            JolokiaRuntimeConfig config,
            JolokiaRecorder recorder) {
        return new JolokiaServerConfigBuildItem(
                recorder.createJolokiaServerConfig(config, buildTimeConfig.path(), applicationInfo.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    JolokiaServerBuildItem createJolokiaServer(
            JolokiaServerConfigBuildItem jolokiaServerConfig,
            JolokiaRecorder recorder) {
        return new JolokiaServerBuildItem(recorder.createJolokiaServer(jolokiaServerConfig.getRuntimeValue()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void startJolokiaServer(
            JolokiaServerBuildItem jolokiaServer,
            JolokiaRuntimeConfig runtimeConfig,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean,
            JolokiaRecorder recorder) {
        recorder.startJolokiaServer(jolokiaServer.getRuntimeValue(), runtimeConfig);
        syntheticBean.produce(SyntheticBeanBuildItem.configure(CamelQuarkusJolokiaServer.class)
                .scope(ApplicationScoped.class)
                .runtimeValue(recorder.createJolokiaServerBean(jolokiaServer.getRuntimeValue()))
                .setRuntimeInit()
                .done());
    }

    @BuildStep(onlyIfNot = { IsNormal.class, IsDevelopment.class })
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerJolokiaServerShutdownHook(
            JolokiaServerBuildItem jolokiaServer,
            ShutdownContextBuildItem shutdownContextBuildItem,
            JolokiaRecorder recorder) {
        recorder.registerJolokiaServerShutdownHook(jolokiaServer.getRuntimeValue(), shutdownContextBuildItem);
    }

    @BuildStep(onlyIf = JolokiaManagementEndpointEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void createManagementRoute(
            JolokiaServerConfigBuildItem jolokiaServerConfig,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BodyHandlerBuildItem bodyHandler,
            Capabilities capabilities,
            BuildProducer<RouteBuildItem> routes,
            JolokiaBuildTimeConfig buildTimeConfig,
            JolokiaRecorder recorder) {

        if (capabilities.isPresent(Capability.VERTX_HTTP)) {
            String jolokiaEndpointPath = nonApplicationRootPathBuildItem.resolvePath(buildTimeConfig.path());
            routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .management()
                    .routeFunction(buildTimeConfig.path() + "/*", recorder.route(bodyHandler.getHandler()))
                    .handler(recorder.getHandler(jolokiaServerConfig.getRuntimeValue(), jolokiaEndpointPath))
                    .build());
        }
    }

    @BuildStep(onlyIf = { IsNormal.class, ExposeContainerPortEnabled.class })
    KubernetesPortBuildItem configureJolokiaKubernetesPort() {
        return KubernetesPortBuildItem.fromRuntimeConfiguration("jolokia", "quarkus.camel.jolokia.server.port", 8778, true);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void warnJvmInNative(JvmOnlyRecorder recorder) {
        JvmOnlyRecorder.warnJvmInNative(LOG, FEATURE);
        recorder.warnJvmInNative(FEATURE);
    }

    static final class JolokiaEnabled implements BooleanSupplier {
        JolokiaBuildTimeConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.enabled();
        }
    }

    static final class JolokiaManagementEndpointEnabled implements BooleanSupplier {
        JolokiaBuildTimeConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.registerManagementEndpoint();
        }
    }

    static final class ExposeContainerPortEnabled implements BooleanSupplier {
        JolokiaBuildTimeConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.kubernetes().exposeContainerPort();
        }
    }
}
