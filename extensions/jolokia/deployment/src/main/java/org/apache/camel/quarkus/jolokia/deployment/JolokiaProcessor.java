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

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

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
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.NativeMonitoringBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathVisit;
import io.quarkus.paths.PathVisitor;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.quarkus.jolokia.CamelQuarkusJolokiaServer;
import org.apache.camel.quarkus.jolokia.JolokiaRecorder;
import org.apache.camel.quarkus.jolokia.config.JolokiaBuildTimeConfig;
import org.apache.camel.quarkus.jolokia.config.JolokiaRuntimeConfig;
import org.apache.camel.quarkus.jolokia.devmode.DevModeJolokiaServerShutdownListener;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jolokia.server.core.service.api.LogHandler;
import org.jolokia.server.core.service.api.Restrictor;
import org.jolokia.server.core.service.impl.QuietLogHandler;
import org.jolokia.server.core.service.serializer.Serializer;
import org.jolokia.service.history.History;
import org.jolokia.service.history.HistoryMBean;

@BuildSteps(onlyIf = JolokiaProcessor.JolokiaEnabled.class)
public class JolokiaProcessor {
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
            LaunchModeBuildItem launchMode,
            JolokiaServerBuildItem jolokiaServer,
            JolokiaRuntimeConfig runtimeConfig,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean,
            JolokiaRecorder recorder) {
        recorder.startJolokiaServer(jolokiaServer.getRuntimeValue(), runtimeConfig);

        SyntheticBeanBuildItem.ExtendedBeanConfigurator beanConfigurator = SyntheticBeanBuildItem
                .configure(CamelQuarkusJolokiaServer.class)
                .scope(ApplicationScoped.class)
                .runtimeValue(recorder.createJolokiaServerBean(jolokiaServer.getRuntimeValue()))
                .setRuntimeInit();

        if (launchMode.getLaunchMode().equals(LaunchMode.DEVELOPMENT)) {
            // Unremovable in dev mode so it can be used in DevModeJolokiaServerShutdownListener
            beanConfigurator.unremovable();
        }

        syntheticBean.produce(beanConfigurator.done());
    }

    @BuildStep(onlyIfNot = { IsNormal.class, IsDevelopment.class })
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerJolokiaServerShutdownHook(
            JolokiaServerBuildItem jolokiaServer,
            ShutdownContextBuildItem shutdownContextBuildItem,
            JolokiaRecorder recorder) {
        recorder.registerJolokiaServerShutdownHook(jolokiaServer.getRuntimeValue(), shutdownContextBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    ShutdownListenerBuildItem devModeJolokiaServerShutdownListener() {
        return new ShutdownListenerBuildItem(new DevModeJolokiaServerShutdownListener());
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

    @BuildStep
    IndexDependencyBuildItem indexJolokiaServerCore() {
        return new IndexDependencyBuildItem("org.jolokia", "jolokia-server-core");
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void jolokiaNativeSupport(
            CombinedIndexBuildItem combinedIndex,
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResource,
            BuildProducer<NativeMonitoringBuildItem> nativeMonitoring) {

        // Enable JMX server, without this some Java runtime MBean attributes & operations are not available
        nativeMonitoring.produce(new NativeMonitoringBuildItem(NativeConfig.MonitoringOption.JMXSERVER));

        // Register Jolokia service support
        configureJolokiaServiceNativeSupport(curateOutcome, reflectiveClass, nativeImageResource);

        // Register Jolokia MBean reflective support
        reflectiveClass.produce(
                ReflectiveClassBuildItem
                        .builder(Serializer.class, History.class, HistoryMBean.class)
                        .methods(true)
                        .build());

        // Register custom (non-OSGi) Jolokia Restrictor impls for reflection
        Set<String> jolokiaRestrictorClasses = combinedIndex.getIndex()
                .getAllKnownImplementors(Restrictor.class)
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .filter(className -> !className.startsWith("org.jolokia.server.core.osgi"))
                .collect(Collectors.toSet());

        jolokiaRestrictorClasses.add(CamelJolokiaRestrictor.class.getName());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(jolokiaRestrictorClasses.toArray(new String[0])).build());

        // Register custom LogHandler classes for reflection
        Set<String> jolokiaLogHandlerClasses = combinedIndex.getIndex()
                .getAllKnownImplementors(LogHandler.class)
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .filter(className -> !className.startsWith("org.jolokia"))
                .collect(Collectors.toSet());

        jolokiaLogHandlerClasses.add(QuietLogHandler.class.getName());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(jolokiaLogHandlerClasses.toArray(new String[0])).build());

        // Include Jolokia static configuration defaults
        nativeImageResource.produce(new NativeImageResourceBuildItem("default-jolokia-agent.properties"));
        nativeImageResource.produce(new NativeImageResourceBuildItem("version.properties"));
    }

    private static void configureJolokiaServiceNativeSupport(
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResource) {

        Set<String> jolokiaServiceIncludes = Collections.singleton("META-INF/jolokia/*");
        PathFilter pathFilter = PathFilter.forIncludes(jolokiaServiceIncludes);

        Set<ResolvedDependency> jolokiaDependencies = curateOutcome.getApplicationModel()
                .getRuntimeDependencies()
                .stream()
                .filter(dependency -> dependency.getGroupId().equals("org.jolokia"))
                .collect(Collectors.toUnmodifiableSet());

        Set<String> jolokiaReflectiveClasses = new HashSet<>();
        for (ResolvedDependency dependency : jolokiaDependencies) {
            dependency.getContentTree(pathFilter).walk(new PathVisitor() {
                @Override
                public void visitPath(PathVisit visit) {
                    String resourcePath = StringHelper.after(visit.getPath().toString(), "/");
                    nativeImageResource.produce(new NativeImageResourceBuildItem(resourcePath));

                    try {
                        for (String line : Files.readAllLines(visit.getPath())) {
                            if (ObjectHelper.isEmpty(line) || line.startsWith("#")) {
                                continue;
                            }

                            String serviceClass;
                            if (line.indexOf(',') > -1) {
                                serviceClass = line.substring(0, line.indexOf(',')).trim();
                            } else {
                                serviceClass = line.trim();
                            }

                            jolokiaReflectiveClasses.add(serviceClass);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        if (!jolokiaReflectiveClasses.isEmpty()) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(jolokiaReflectiveClasses.toArray(new String[0])).build());
        }
    }

    @BuildStep
    void registerServices(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(Serializer.class.getName()));
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
