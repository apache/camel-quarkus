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

import javax.enterprise.inject.Vetoed;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.recording.RecorderContext;

import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckRepository;
import org.apache.camel.impl.health.DefaultHealthCheckRegistry;
import org.apache.camel.microprofile.health.AbstractCamelMicroProfileHealthCheck;
import org.apache.camel.quarkus.component.microprofile.health.runtime.CamelMicroProfileHealthConfig;
import org.apache.camel.quarkus.component.microprofile.health.runtime.CamelMicroProfileHealthRecorder;
import org.apache.camel.quarkus.core.deployment.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelSupport;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class MicroProfileHealthProcessor {

    private static final DotName CAMEL_HEALTH_CHECK_DOTNAME = DotName.createSimple(HealthCheck.class.getName());
    private static final DotName CAMEL_HEALTH_CHECK_REPOSITORY_DOTNAME = DotName.createSimple(HealthCheckRepository.class.getName());
    private static final DotName MICROPROFILE_LIVENESS_DOTNAME = DotName.createSimple(Liveness.class.getName());
    private static final DotName MICROPROFILE_READINESS_DOTNAME = DotName.createSimple(Readiness.class.getName());
    private static final DotName VETOED_DOTNAME = DotName.createSimple(Vetoed.class.getName());
    private static final String FEATURE = "camel-microprofile-health";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    List<CamelBeanBuildItem> camelHealthDiscovery(
        CombinedIndexBuildItem combinedIndex,
        RecorderContext recorderContext,
        CamelMicroProfileHealthRecorder recorder,
        CamelMicroProfileHealthConfig config) {

        List<CamelBeanBuildItem> buildItems = new ArrayList<>();
        if (config.enabled) {
            IndexView index = combinedIndex.getIndex();
            Collection<ClassInfo> healthChecks = index.getAllKnownImplementors(CAMEL_HEALTH_CHECK_DOTNAME);
            Collection<ClassInfo> healthCheckRepositories = index.getAllKnownImplementors(CAMEL_HEALTH_CHECK_REPOSITORY_DOTNAME);

            // Create CamelBeanBuildItem to bind instances of HealthCheck to the camel registry
            healthChecks.stream()
                .filter(CamelSupport::isConcrete)
                .filter(CamelSupport::isPublic)
                .filter(ClassInfo::hasNoArgsConstructor)
                .map(classInfo -> {
                    Class<?> clazz = recorderContext.classProxy(classInfo.toString());
                    return new CamelBeanBuildItem(classInfo.simpleName(), HealthCheck.class, recorder.createHealthCheck(clazz));
                })
                .forEach(buildItems::add);

            // Create CamelBeanBuildItem to bind instances of HealthCheckRepository to the camel registry
            healthCheckRepositories.stream()
                .filter(CamelSupport::isConcrete)
                .filter(CamelSupport::isPublic)
                .filter(ClassInfo::hasNoArgsConstructor)
                .filter(classInfo -> !classInfo.simpleName().equals(DefaultHealthCheckRegistry.class.getSimpleName()))
                .map(classInfo -> {
                    Class<?> clazz = recorderContext.classProxy(classInfo.toString());
                    return new CamelBeanBuildItem(classInfo.simpleName(), HealthCheckRepository.class, recorder.createHealthCheckRepository(clazz));
                })
                .forEach(buildItems::add);
        }

        return buildItems;
    }

    @BuildStep
    void disableCamelMicroProfileHealthChecks(BuildProducer<AnnotationsTransformerBuildItem> transformers, CamelMicroProfileHealthConfig config) {
        if (!config.enabled) {
            // Veto the Camel MicroProfile checks to disable them
            transformers.produce(new AnnotationsTransformerBuildItem(context -> {
                if (context.isClass()) {
                    AnnotationTarget target = context.getTarget();
                    if (isCamelMicroProfileHealthCheck(target.asClass())) {
                        AnnotationInstance annotationInstance = AnnotationInstance.create(VETOED_DOTNAME, target, new AnnotationValue[0]);
                        context.transform().add(annotationInstance).done();
                    }
                }
            }));
        }
    }

    private boolean isCamelMicroProfileHealthCheck(ClassInfo classInfo) {
        String className = classInfo.name().toString();
        return ((className.startsWith(AbstractCamelMicroProfileHealthCheck.class.getPackage().getName())) &&
                (classInfo.classAnnotation(MICROPROFILE_LIVENESS_DOTNAME) != null || classInfo.classAnnotation(MICROPROFILE_READINESS_DOTNAME) != null));
    }
}
