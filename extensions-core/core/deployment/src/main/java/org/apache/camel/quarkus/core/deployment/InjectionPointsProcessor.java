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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.QualifierRegistrarBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.core.CamelCapabilities;
import org.apache.camel.quarkus.core.CamelRecorder;
import org.apache.camel.quarkus.core.InjectionPointsRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeTaskBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

public class InjectionPointsProcessor {
    private static final Logger LOGGER = Logger.getLogger(InjectionPointsProcessor.class);

    private static final DotName ANNOTATION_NAME_NAMED = DotName.createSimple(
            Named.class.getName());
    private static final DotName INTERFACE_NAME_COMPONENT = DotName.createSimple(
            Component.class.getName());
    private static final DotName ENDPOINT_INJECT_ANNOTATION = DotName
            .createSimple(EndpointInject.class.getName());
    private static final DotName PRODUCE_ANNOTATION = DotName
            .createSimple(Produce.class.getName());
    private static final DotName TEST_SUPPORT_CLASS_NAME = DotName
            .createSimple("org.apache.camel.quarkus.test.CamelQuarkusTestSupport");

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
                final List<Type> types = target.parameterTypes();

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

    @BuildStep
    void annotationsTransformers(
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformers) {

        annotationsTransformers.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == Kind.FIELD || kind == Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext ctx) {

                final AnnotationTarget target = ctx.getTarget();
                switch (target.kind()) {
                case FIELD: {
                    final FieldInfo fieldInfo = target.asField();
                    if (fieldInfo.annotation(ENDPOINT_INJECT_ANNOTATION) != null
                            || fieldInfo.annotation(PRODUCE_ANNOTATION) != null) {
                        ctx.transform().add(Inject.class).done();
                    }
                    break;
                }
                case METHOD: {
                    final MethodInfo methodInfo = target.asMethod();
                    fail(methodInfo, ENDPOINT_INJECT_ANNOTATION);
                    fail(methodInfo, PRODUCE_ANNOTATION);
                    break;
                }
                default:
                    throw new IllegalStateException("Expected only field or method, got " + target.kind());
                }

            }

        }));

    }

    static void fail(final MethodInfo methodInfo, DotName annotType) {
        if (methodInfo.annotation(annotType) != null) {
            // See https://github.com/apache/camel-quarkus/issues/2579
            throw new IllegalStateException(
                    "@" + annotType + " is only supported on fields. Remove it from "
                            + methodInfo + " in " + methodInfo.declaringClass().name());
        }
    }

    @BuildStep
    void qualifierRegistrars(
            BuildProducer<QualifierRegistrarBuildItem> qualifierRegistrars) {
        qualifierRegistrars.produce(new QualifierRegistrarBuildItem(new QualifierRegistrar() {

            @Override
            public Map<DotName, Set<String>> getAdditionalQualifiers() {
                Map<DotName, Set<String>> result = new LinkedHashMap<DotName, Set<String>>();
                result.put(ENDPOINT_INJECT_ANNOTATION, Collections.emptySet());
                result.put(PRODUCE_ANNOTATION, Collections.emptySet());
                return Collections.unmodifiableMap(result);
            }
        }));
    }

    @Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
    @BuildStep
    void syntheticBeans(
            CamelRecorder recorder,
            CombinedIndexBuildItem index,
            List<CapabilityBuildItem> capabilities,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions) {

        Set<String> alreadyCreated = new HashSet<>();

        for (AnnotationInstance annot : index.getIndex().getAnnotations(ENDPOINT_INJECT_ANNOTATION)) {
            final AnnotationTarget target = annot.target();
            switch (target.kind()) {
            case FIELD: {
                final FieldInfo field = target.asField();
                if (!excludeTestSyntheticBeanDuplicities(annot, alreadyCreated, field.declaringClass(), index.getIndex())) {
                    endpointInjectBeans(recorder, syntheticBeans, index.getIndex(), annot, field.type().name());
                }
                break;
            }
            case METHOD: {
                final MethodInfo methodInfo = target.asMethod();
                fail(methodInfo, ENDPOINT_INJECT_ANNOTATION);
                break;
            }
            default:
                throw new IllegalStateException("Expected field, got " + target.kind());
            }
        }

        AtomicReference<Boolean> beanCapabilityAvailable = new AtomicReference<>();

        for (AnnotationInstance annot : index.getIndex().getAnnotations(PRODUCE_ANNOTATION)) {
            final AnnotationTarget target = annot.target();
            switch (target.kind()) {
            case FIELD: {
                final FieldInfo field = target.asField();
                if (!excludeTestSyntheticBeanDuplicities(annot, alreadyCreated, field.declaringClass(), index.getIndex())) {
                    produceBeans(recorder, capabilities, syntheticBeans, proxyDefinitions, beanCapabilityAvailable,
                            index.getIndex(), annot, field.type().name(), field.name(), field.declaringClass().name());
                }
                break;
            }
            case METHOD: {
                final MethodInfo methodInfo = target.asMethod();
                fail(methodInfo, PRODUCE_ANNOTATION);
                break;
            }
            default:
                throw new IllegalStateException("Expected field, got " + target.kind());
            }
        }
    }

    private boolean excludeTestSyntheticBeanDuplicities(AnnotationInstance annot, Set<String> alreadyCreated,
            ClassInfo declaringClass, IndexView index) {
        String identifier = annot.toString(false) + ":" + getTargetClass(annot).toString();

        if (extendsCamelQuarkusTest(declaringClass, index)) {
            if (alreadyCreated.contains(identifier)) {
                return true;
            } else {
                alreadyCreated.add(identifier);
            }
        }
        return false;
    }

    private DotName getTargetClass(AnnotationInstance annot) {
        switch (annot.target().kind()) {
        case FIELD:
            return annot.target().asField().type().name();
        case METHOD:
            return annot.target().asMethod().returnType().name();
        default:
            return null;
        }
    }

    private boolean extendsCamelQuarkusTest(ClassInfo declaringClass, IndexView indexView) {
        if (declaringClass == null) {
            return false;
        }

        if (TEST_SUPPORT_CLASS_NAME.equals(declaringClass.name())) {
            return true;
        }

        //iterate over parent until found CamelQuarkusTest or null
        return (declaringClass.superName() != null &&
                extendsCamelQuarkusTest(indexView.getClassByName(declaringClass.superName()), indexView));
    }

    void produceBeans(CamelRecorder recorder, List<CapabilityBuildItem> capabilities,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions,
            AtomicReference<Boolean> beanCapabilityAvailable,
            IndexView index,
            AnnotationInstance annot, final DotName fieldType, String annotationTarget, DotName declaringClass) {
        try {
            Class<?> clazz = Class.forName(fieldType.toString(), false,
                    Thread.currentThread().getContextClassLoader());
            if (ProducerTemplate.class.isAssignableFrom(clazz)) {
                syntheticBeans.produce(
                        SyntheticBeanBuildItem
                                .configure(fieldType)
                                .setRuntimeInit().scope(Singleton.class)
                                .supplier(
                                        recorder.createProducerTemplate(resolveAnnotValue(index, annot)))
                                .addQualifier(annot)
                                .done());
                /*
                 * Note that ProducerTemplate injection points not having @EndpointInject are produced via
                 * CamelProducers.camelProducerTemplate()
                 */
            } else if (FluentProducerTemplate.class.isAssignableFrom(clazz)) {
                syntheticBeans.produce(
                        SyntheticBeanBuildItem
                                .configure(fieldType)
                                .setRuntimeInit().scope(Singleton.class)
                                .supplier(
                                        recorder.createFluentProducerTemplate(resolveAnnotValue(index, annot)))
                                .addQualifier(annot)
                                .done());
                /*
                 * Note that FluentProducerTemplate injection points not having @EndpointInject are produced via
                 * CamelProducers.camelFluentProducerTemplate()
                 */
            } else if (clazz.isInterface()) {
                /* Requires camel-quarkus-bean */

                if (beanCapabilityAvailable.get() == null) {
                    beanCapabilityAvailable.set(capabilities.stream().map(CapabilityBuildItem::getName)
                            .anyMatch(feature -> CamelCapabilities.BEAN.equals(feature)));
                }
                if (!beanCapabilityAvailable.get()) {
                    throw new IllegalStateException(
                            "Add camel-quarkus-bean dependency to be able to use @org.apache.camel.Produce on fields with interface type: "
                                    + fieldType.toString()
                                    + " " + annotationTarget + " in "
                                    + declaringClass.toString());
                }

                proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(fieldType.toString()));
                syntheticBeans.produce(
                        SyntheticBeanBuildItem
                                .configure(fieldType)
                                .setRuntimeInit().scope(Singleton.class)
                                .supplier(
                                        recorder.produceProxy(clazz, resolveAnnotValue(index, annot)))
                                .addQualifier(annot)
                                .done());
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void endpointInjectBeans(CamelRecorder recorder, BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            IndexView index, AnnotationInstance annot, final DotName fieldType) {
        try {
            Class<?> clazz = Class.forName(fieldType.toString());
            if (Endpoint.class.isAssignableFrom(clazz)) {
                syntheticBeans.produce(
                        SyntheticBeanBuildItem
                                .configure(fieldType)
                                .setRuntimeInit().scope(Singleton.class)
                                .supplier(
                                        recorder.createEndpoint(resolveAnnotValue(index, annot),
                                                (Class<? extends Endpoint>) clazz))
                                .addQualifier(annot)
                                .done());
            } else if (ProducerTemplate.class.isAssignableFrom(clazz)) {
                syntheticBeans.produce(
                        SyntheticBeanBuildItem
                                .configure(fieldType)
                                .setRuntimeInit().scope(Singleton.class)
                                .supplier(
                                        recorder.createProducerTemplate(resolveAnnotValue(index, annot)))
                                .addQualifier(annot)
                                .done());
                /*
                 * Note that ProducerTemplate injection points not having @EndpointInject are produced via
                 * CamelProducers.camelProducerTemplate()
                 */
            } else if (FluentProducerTemplate.class.isAssignableFrom(clazz)) {
                syntheticBeans.produce(
                        SyntheticBeanBuildItem
                                .configure(fieldType)
                                .setRuntimeInit().scope(Singleton.class)
                                .supplier(
                                        recorder.createFluentProducerTemplate(resolveAnnotValue(index, annot)))
                                .addQualifier(annot)
                                .done());
                /*
                 * Note that FluentProducerTemplate injection points not having @EndpointInject are produced via
                 * CamelProducers.camelFluentProducerTemplate()
                 */
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private String resolveAnnotValue(IndexView index, AnnotationInstance annot) {
        return annot.valueWithDefault(index).asString();
    }
}
