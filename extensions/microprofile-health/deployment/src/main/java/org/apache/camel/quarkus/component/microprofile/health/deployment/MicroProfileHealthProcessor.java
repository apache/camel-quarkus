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
package org.apache.camel.quarkus.component.microprofile.health.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.smallrye.health.deployment.HealthBuildTimeConfig;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.health.HealthCheckRepository;
import org.apache.camel.impl.health.ConsumersHealthCheckRepository;
import org.apache.camel.impl.health.HealthCheckRegistryRepository;
import org.apache.camel.impl.health.RoutesHealthCheckRepository;
import org.apache.camel.quarkus.component.microprofile.health.runtime.CamelMicroProfileHealthConfig;
import org.apache.camel.quarkus.component.microprofile.health.runtime.CamelMicroProfileHealthRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextCustomizerBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class MicroProfileHealthProcessor {

    private static final DotName CAMEL_HEALTH_CHECK_DOTNAME = DotName.createSimple(HealthCheck.class.getName());
    private static final DotName CAMEL_HEALTH_CHECK_REPOSITORY_DOTNAME = DotName
            .createSimple(HealthCheckRepository.class.getName());
    private static final String FEATURE = "camel-microprofile-health";

    static final class HealthEnabled implements BooleanSupplier {
        CamelMicroProfileHealthConfig camelHealthConfig;
        HealthBuildTimeConfig quarkusHealthConfig;

        @Override
        public boolean getAsBoolean() {
            Boolean mpHealthDisabled = ConfigProvider.getConfig()
                    .getOptionalValue("mp.health.disable-default-procedures", boolean.class)
                    .orElse(false);

            Boolean camelHealthEnabled = ConfigProvider.getConfig()
                    .getOptionalValue("camel.health.enabled", boolean.class)
                    .orElse(true);

            return !mpHealthDisabled && camelHealthEnabled && camelHealthConfig.enabled
                    && quarkusHealthConfig.extensionsEnabled;
        }
    }

    static final class HealthRegistryEnabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ConfigProvider.getConfig()
                    .getOptionalValue("camel.health.registryEnabled", boolean.class)
                    .orElse(true);
        }
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = HealthEnabled.class)
    CamelServicePatternBuildItem excludeDefaultHealthCheckRegistry() {
        // Prevent camel main from overwriting the HealthCheckRegistry configured by this extension
        return new CamelServicePatternBuildItem(CamelServiceDestination.DISCOVERY, false,
                "META-INF/services/org/apache/camel/" + HealthCheckRegistry.FACTORY);
    }

    @BuildStep(onlyIf = { HealthEnabled.class, HealthRegistryEnabled.class })
    @Record(ExecutionTime.STATIC_INIT)
    CamelContextCustomizerBuildItem customizeHealthCheckRegistry(CamelMicroProfileHealthRecorder recorder) {
        return new CamelContextCustomizerBuildItem(recorder.createHealthCheckRegistry());
    }

    @BuildStep(onlyIf = HealthEnabled.class)
    List<CamelBeanBuildItem> camelHealthDiscovery(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();
        List<CamelBeanBuildItem> buildItems = new ArrayList<>();
        Collection<ClassInfo> healthChecks = index.getAllKnownImplementors(CAMEL_HEALTH_CHECK_DOTNAME);
        Collection<ClassInfo> healthCheckRepositories = index
                .getAllKnownImplementors(CAMEL_HEALTH_CHECK_REPOSITORY_DOTNAME);

        Config config = ConfigProvider.getConfig();
        Predicate<ClassInfo> healthCheckFilter = classInfo -> {
            String className = classInfo.name().toString();
            if (className.equals(HealthCheckRegistryRepository.class.getName())) {
                // HealthCheckRegistryRepository is created internally by Camel
                return false;
            }

            if (className.equals(RoutesHealthCheckRepository.class.getName())) {
                return config.getOptionalValue("camel.health.routesEnabled", boolean.class).orElse(true);
            }

            if (className.equals(ConsumersHealthCheckRepository.class.getName())) {
                return config.getOptionalValue("camel.health.consumersEnabled", boolean.class).orElse(true);
            }

            return true;
        };

        // Create CamelBeanBuildItem to bind instances of HealthCheck to the camel registry
        healthChecks.stream()
                .filter(CamelSupport::isConcrete)
                .filter(CamelSupport::isPublic)
                .filter(ClassInfo::hasNoArgsConstructor)
                .filter(healthCheckFilter)
                .map(this::createHealthCamelBeanBuildItem)
                .forEach(buildItems::add);

        // Create CamelBeanBuildItem to bind instances of HealthCheckRepository to the camel registry
        healthCheckRepositories.stream()
                .filter(CamelSupport::isConcrete)
                .filter(CamelSupport::isPublic)
                .filter(ClassInfo::hasNoArgsConstructor)
                .filter(healthCheckFilter)
                .map(this::createHealthCamelBeanBuildItem)
                .forEach(buildItems::add);

        return buildItems;
    }

    private CamelBeanBuildItem createHealthCamelBeanBuildItem(ClassInfo classInfo) {
        String beanName;
        String className = classInfo.name().toString();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            Class<?> clazz = classLoader.loadClass(className);
            Object health = clazz.getDeclaredConstructor().newInstance();
            if (health instanceof HealthCheck) {
                beanName = ((HealthCheck) health).getId();
            } else if (health instanceof HealthCheckRepository) {
                beanName = ((HealthCheckRepository) health).getId();
            } else {
                throw new IllegalArgumentException("Unknown health type " + className);
            }

            if (ObjectHelper.isEmpty(beanName)) {
                beanName = className;
            }

            return new CamelBeanBuildItem(beanName, className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
