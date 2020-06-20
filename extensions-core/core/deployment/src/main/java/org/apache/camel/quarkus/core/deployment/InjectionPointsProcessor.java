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
package org.apache.camel.quarkus.core.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Named;
import javax.inject.Singleton;

import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.apache.camel.Component;
import org.apache.camel.quarkus.core.InjectionPointsRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeTaskBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

public class InjectionPointsProcessor {
    private static final Logger LOGGER = Logger.getLogger(InjectionPointsProcessor.class);

    private static final DotName ANNOTATION_NAME_NAMED = DotName.createSimple(
            Named.class.getName());
    private static final DotName INTERFACE_NAME_COMPONENT = DotName.createSimple(
            Component.class.getName());

    private static SyntheticBeanBuildItem syntheticBean(DotName name, Supplier<?> creator) {
        return SyntheticBeanBuildItem.configure(name)
                .supplier(creator)
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .done();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void injectedComponents(
            CombinedIndexBuildItem index,
            InjectionPointsRecorder recorder,
            BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<CamelRuntimeTaskBuildItem> runtimeTasks,
            BuildProducer<BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem> beanConfigurator) {

        final Collection<ClassInfo> components = index.getIndex().getAllKnownImplementors(INTERFACE_NAME_COMPONENT);
        final Set<String> created = new HashSet<>();

        for (InjectionPointInfo injectionPoint : beanRegistrationPhase.getContext().get(BuildExtension.Key.INJECTION_POINTS)) {
            if (injectionPoint.getTarget().kind() == AnnotationTarget.Kind.FIELD) {
                FieldInfo target = injectionPoint.getTarget().asField();

                if (!created.add(target.type().name().toString())) {
                    continue;
                }

                if (components.stream().anyMatch(ci -> Objects.equals(ci.name(), target.type().name()))) {
                    final AnnotationInstance named = target.annotation(ANNOTATION_NAME_NAMED);
                    final String componentName = named == null ? target.name() : named.value().asString();
                    final Supplier<?> creator = recorder.componentSupplier(componentName, target.type().toString());

                    LOGGER.debugf("Creating synthetic component bean [name=%s, type=%s]", componentName, target.type().name());

                    syntheticBeans.produce(syntheticBean(target.type().name(), creator));
                }
            }

            if (injectionPoint.getTarget().kind() == AnnotationTarget.Kind.METHOD) {
                final MethodInfo target = injectionPoint.getTarget().asMethod();
                final List<Type> types = target.parameters();

                for (int i = 0; i < types.size(); i++) {
                    Type type = types.get(0);

                    if (!created.add(type.name().toString())) {
                        continue;
                    }

                    if (components.stream().anyMatch(ci -> Objects.equals(ci.name(), type.name()))) {
                        final AnnotationInstance named = target.annotation(ANNOTATION_NAME_NAMED);
                        final String componentName = named == null ? target.parameterName(i) : named.value().asString();
                        final Supplier<?> creator = recorder.componentSupplier(componentName, type.toString());

                        LOGGER.debugf("Creating synthetic component bean [name=%s, type=%s]", componentName, type.name());

                        syntheticBeans.produce(syntheticBean(type.name(), creator));
                    }
                }
            }
        }

        // Ensure the task is executed before the runtime is assembled
        runtimeTasks.produce(new CamelRuntimeTaskBuildItem("components-injection"));

        // Methods using BeanRegistrationPhaseBuildItem should return/produce a BeanConfiguratorBuildItem
        // otherwise the build step may be processed at the wrong time.
        //
        // See BeanRegistrationPhaseBuildItem javadoc
        beanConfigurator.produce(new BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem());
    }
}
