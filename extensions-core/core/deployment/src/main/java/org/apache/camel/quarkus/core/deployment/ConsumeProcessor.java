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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.decorator.Decorator;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.Interceptor;
import javax.inject.Named;
import javax.inject.Singleton;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.Consume;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.quarkus.core.ConsumeRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.support.common.CamelCapabilities;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

/**
 * Support for Camel {@link Consume} annotation.
 */
public class ConsumeProcessor {

    private static final DotName CONSUME_ANNOTATION = DotName.createSimple(Consume.class.getName());
    private static final DotName NAMED_ANNOTATION = DotName.createSimple(Named.class.getName());
    /**
     * Based on https://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#builtin_scopes
     * the list is not 100% complete, but hopefully it will suffice for our purposes
     */
    public static final Set<DotName> BEAN_DEFINING_ANNOTATIONS = new HashSet<DotName>(Arrays.asList(
            DotName.createSimple(ApplicationScoped.class.getName()),
            DotName.createSimple(SessionScoped.class.getName()),
            DotName.createSimple(ConversationScoped.class.getName()),
            DotName.createSimple(RequestScoped.class.getName()),
            DotName.createSimple(Interceptor.class.getName()),
            DotName.createSimple(Decorator.class.getName()),
            DotName.createSimple(Dependent.class.getName()),
            DotName.createSimple(Singleton.class.getName())));

    @BuildStep
    void annotationsTransformers(
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformers) {

        annotationsTransformers.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext ctx) {
                final ClassInfo classInfo = ctx.getTarget().asClass();
                if (hasConsumeMethod(classInfo)) {
                    /* If there is @Consume on a method, make the declaring class a named injectable bean */
                    String beanName = namedValue(classInfo);
                    final Transformation transform = ctx.transform();
                    if (!classInfo.annotations().keySet().stream().anyMatch(BEAN_DEFINING_ANNOTATIONS::contains)) {
                        /* Only add @Singleton if there is no other bean defining annotation yet */
                        transform.add(Singleton.class);
                    }

                    if (beanName == null) {
                        beanName = ConsumeProcessor.uniqueBeanName(classInfo);
                        transform.add(Named.class, AnnotationValue.createStringValue("value", beanName));
                    }

                    transform.done();
                }
            }
        }));

    }

    static String namedValue(ClassInfo classInfo) {
        String beanName = null;
        final AnnotationInstance named = classInfo.classAnnotation(NAMED_ANNOTATION);
        if (named != null) {
            if (named.value() != null) {
                beanName = named.value().asString();
            }
            if (beanName == null) {
                /* default bean name */
                beanName = ConsumeProcessor.firstLower(classInfo.simpleName());
            }
        }
        return beanName;
    }

    static boolean hasConsumeMethod(ClassInfo classInfo) {
        for (MethodInfo methodInfo : classInfo.methods()) {
            if (methodInfo.annotation(CONSUME_ANNOTATION) != null) {
                return true;
            }
        }
        return false;
    }

    @Record(value = ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateConsumeRoutes(
            ConsumeRecorder recorder,
            CombinedIndexBuildItem index,
            List<CapabilityBuildItem> capabilities,
            CamelContextBuildItem camelContext) {

        final Collection<AnnotationInstance> consumeAnnotations = index.getIndex().getAnnotations(CONSUME_ANNOTATION);
        if (!consumeAnnotations.isEmpty()) {
            final RuntimeValue<RoutesDefinition> routesDefinition = recorder.createRoutesDefinition();

            final boolean beanCapabilityAvailable = capabilities.stream().map(CapabilityBuildItem::getName)
                    .anyMatch(feature -> CamelCapabilities.BEAN.equals(feature));

            for (AnnotationInstance annot : consumeAnnotations) {
                final AnnotationTarget target = annot.target();
                switch (target.kind()) {
                case METHOD: {
                    final MethodInfo methodInfo = target.asMethod();
                    final String uri = annot.value().asString();
                    final ClassInfo declaringClass = methodInfo.declaringClass();
                    if (uri.isEmpty()) {
                        throw new IllegalStateException("@" + Consume.class.getName()
                                + " requires a Camel endpoint URI in its value, e.g. @Consume(\"direct:myDirect\"): "
                                + methodInfo
                                + " in " + declaringClass.name());
                    }
                    if (!beanCapabilityAvailable) {
                        throw new IllegalStateException(
                                "Add camel-quarkus-bean dependency to be able to use @" + Consume.class.getName()
                                        + " on method:"
                                        + methodInfo
                                        + " in " + declaringClass.name());
                    }
                    String beanName = namedValue(declaringClass);
                    if (beanName == null) {
                        beanName = ConsumeProcessor.uniqueBeanName(declaringClass);
                    }
                    recorder.addConsumeRoute(routesDefinition, uri, beanName, methodInfo.name());
                    break;
                }
                default:
                    throw new IllegalStateException("Expected method, got " + target.kind() + ": " + target);
                }
            }
            recorder.addConsumeRoutesToContext(camelContext.getCamelContext(), routesDefinition);
        }
    }

    @BuildStep
    void unremovables(
            CombinedIndexBuildItem index,
            BuildProducer<UnremovableBeanBuildItem> unremovables,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        final Collection<AnnotationInstance> consumeAnnotations = index.getIndex().getAnnotations(CONSUME_ANNOTATION);
        if (!consumeAnnotations.isEmpty()) {
            final Set<DotName> declaringClasses = new LinkedHashSet<DotName>();
            for (AnnotationInstance annot : consumeAnnotations) {
                final AnnotationTarget target = annot.target();
                switch (target.kind()) {
                case METHOD: {
                    final MethodInfo methodInfo = target.asMethod();
                    declaringClasses.add(methodInfo.declaringClass().name());
                    break;
                }
                default:
                    throw new IllegalStateException("Expected method, got " + target.kind() + ": " + target);
                }
            }
            unremovables.produce(UnremovableBeanBuildItem.beanTypes(declaringClasses));

            reflectiveClasses.produce(
                    new ReflectiveClassBuildItem(
                            true,
                            false,
                            declaringClasses.stream()
                                    .map(DotName::toString)
                                    .toArray(String[]::new)));
        }
    }

    static String uniqueBeanName(ClassInfo classInfo) {
        return firstLower(classInfo.simpleName()) + classInfo.name().hashCode();
    }

    static String firstLower(String str) {
        char c[] = str.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }

}
