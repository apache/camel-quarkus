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
package org.apache.camel.quarkus.component.console.deployment;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import org.apache.camel.quarkus.component.console.CamelConsoleConfig;
import org.apache.camel.quarkus.component.console.CamelConsoleRecorder;
import org.apache.camel.quarkus.core.JvmOnlyRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

@BuildSteps(onlyIf = ConsoleProcessor.CamelConsoleEnabled.class)
class ConsoleProcessor {
    private static final Logger LOG = Logger.getLogger(ConsoleProcessor.class);
    private static final String FEATURE = "camel-console";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    CamelServicePatternBuildItem devConsoleServicePattern() {
        return new CamelServicePatternBuildItem(CamelServiceDestination.DISCOVERY, true,
                "META-INF/services/org/apache/camel/dev-console/*");
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void initDevConsoleRegistry(
            CamelContextBuildItem camelContext,
            CamelConsoleRecorder recorder) {
        recorder.initDevConsoleRegistry(camelContext.getCamelContext());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createManagementRoute(
            CamelContextBuildItem camelContext,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BuildProducer<RouteBuildItem> routes,
            CamelConsoleConfig config,
            CamelConsoleRecorder recorder) {

        if (canExposeManagementEndpoint(config)) {
            Consumer<Route> route = recorder.route();
            Handler<RoutingContext> handler = recorder.getHandler(camelContext.getCamelContext());

            routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .management()
                    .routeFunction(config.path(), route)
                    .handler(handler)
                    .build());

            routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .management()
                    .routeFunction(config.path() + "/:id", route)
                    .handler(handler)
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

    private static boolean canExposeManagementEndpoint(CamelConsoleConfig config) {
        return (LaunchMode.current().isDevOrTest()
                && config.exposureMode() == CamelConsoleConfig.ExposureMode.DEV_TEST)
                || config.exposureMode().equals(CamelConsoleConfig.ExposureMode.ALL);
    }

    static final class CamelConsoleEnabled implements BooleanSupplier {
        CamelConsoleConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.enabled() || ConfigProvider.getConfig()
                    .getOptionalValue("camel.main.dev-console-enabled", Boolean.class)
                    .orElse(false);
        }
    }
}
