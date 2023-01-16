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

package org.apache.camel.quarkus.dsl.js.deployment;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.quarkus.arc.Components;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.NamedNode;
import org.apache.camel.builder.DataFormatClause;
import org.apache.camel.builder.ExpressionClause;
import org.apache.camel.dsl.js.JavaScriptDSL;
import org.apache.camel.model.Block;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.NoOutputDefinition;
import org.apache.camel.model.OptionalIdentifiedDefinition;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.model.rest.RestSecurityDefinition;
import org.apache.camel.model.transformer.TransformerDefinition;
import org.apache.camel.model.validator.ValidatorDefinition;
import org.apache.camel.quarkus.dsl.js.runtime.JavaScriptDslBiConsumer;
import org.apache.camel.quarkus.dsl.js.runtime.JavaScriptDslBiFunction;
import org.apache.camel.quarkus.dsl.js.runtime.JavaScriptDslBiPredicate;
import org.apache.camel.quarkus.dsl.js.runtime.JavaScriptDslConsumer;
import org.apache.camel.quarkus.dsl.js.runtime.JavaScriptDslFunction;
import org.apache.camel.quarkus.dsl.js.runtime.JavaScriptDslPredicate;
import org.apache.camel.quarkus.dsl.js.runtime.JavaScriptDslSupplier;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.camel.spi.NamespaceAware;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class JavaScriptDslProcessor {
    private static final List<Class<?>> JAVA_CLASSES = Arrays.asList(
            Character.class,
            Byte.class,
            CharSequence.class, String.class,
            Number.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            // Time
            Date.class,
            Temporal.class,
            Instant.class,
            Duration.class,
            // Containers
            Map.class, HashMap.class, TreeMap.class,
            List.class, ArrayList.class, LinkedList.class,
            Set.class, HashSet.class, TreeSet.class);

    private static final List<Class<?>> CAMEL_REFLECTIVE_CLASSES = Arrays.asList(
            ExchangeFormatter.class,
            RouteDefinition.class,
            ProcessorDefinition.class,
            DataFormatClause.class,
            FromDefinition.class,
            ToDefinition.class,
            ExpressionDefinition.class,
            ProcessDefinition.class,
            ExpressionDefinition.class,
            ExpressionClause.class,
            Exchange.class,
            Message.class,
            ExchangePattern.class,
            Endpoint.class,
            CamelContext.class,
            JsonLibrary.class,
            NamedNode.class,
            OptionalIdentifiedDefinition.class,
            NamespaceAware.class,
            Block.class,
            RestSecurityDefinition.class,
            ValidatorDefinition.class,
            TransformerDefinition.class,
            NoOutputDefinition.class);
    public static final String BUILDER_CLASS_SUFFIX = "Builders";

    @BuildStep(onlyIf = NativeBuild.class)
    void registerReflectiveClasses(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            CombinedIndexBuildItem combinedIndexBuildItem) {

        IndexView view = combinedIndexBuildItem.getIndex();

        for (Class<?> type : CAMEL_REFLECTIVE_CLASSES) {
            DotName name = DotName.createSimple(type.getName());

            if (type.isInterface()) {
                for (ClassInfo info : view.getAllKnownImplementors(name)) {
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, info.name().toString()));
                }
            } else {
                for (ClassInfo info : view.getAllKnownSubclasses(name)) {
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, info.name().toString()));
                }
            }

            reflectiveClass.produce(new ReflectiveClassBuildItem(true, type.isEnum(), type));
        }

        for (Class<?> type : JAVA_CLASSES) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, type));
        }

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, Components.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, JavaScriptDSL.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.apache.camel.converter.jaxp.XmlConverter"));

        Set<String> existingComponents = view.getAllKnownImplementors(Component.class)
                .stream()
                .map(JavaScriptDslProcessor::extractName)
                .collect(Collectors.toSet());

        Set<Class<?>> types = new HashSet<>();
        // Register all public methods of JavaScriptDSL for reflection to be accessible in native mode from a JavaScript resource
        for (Method method : JavaScriptDSL.class.getMethods()) {
            Class<?> declaringClass = method.getDeclaringClass();
            if (!declaringClass.equals(Object.class)) {
                String declaringClassName = declaringClass.getSimpleName();
                // Keep only the methods that are not from builder classes or that are from builder classes of included
                // components
                if (!declaringClassName.endsWith(BUILDER_CLASS_SUFFIX) || existingComponents.contains(
                        declaringClassName.substring(0, declaringClassName.length() - BUILDER_CLASS_SUFFIX.length()))) {
                    Class<?> returnType = method.getReturnType();
                    types.add(returnType);
                    reflectiveMethods.produce(new ReflectiveMethodBuildItem(method));
                    if (declaringClassName.endsWith(BUILDER_CLASS_SUFFIX)) {
                        // Add the return type of the advanced method if any
                        Arrays.stream(returnType.getMethods())
                                .filter(m -> m.getName().equals("advanced") && m.getParameterTypes().length == 0)
                                .findAny()
                                .ifPresent(m -> types.add(m.getReturnType()));
                    }
                }
            }
        }
        // Register all the Camel return types of public methods of the camel reflective classes for reflection to
        // be accessible in native mode from a JavaScript resource
        for (Class<?> c : CAMEL_REFLECTIVE_CLASSES) {
            for (Method method : c.getMethods()) {
                if (!method.getDeclaringClass().equals(Object.class)) {
                    Class<?> returnType = method.getReturnType();
                    if (returnType.getPackageName().startsWith("org.apache.camel.")
                            && !CAMEL_REFLECTIVE_CLASSES.contains(returnType)) {
                        types.add(returnType);
                    }
                }
            }
        }
        // Allow access to methods by reflection to be accessible in native mode from a JavaScript resource
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, false, types.toArray(new Class<?>[0])));
        // Register for reflection the runtime implementation of the main functional interfaces.
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, JavaScriptDslBiConsumer.class, JavaScriptDslBiFunction.class,
                        JavaScriptDslBiPredicate.class, JavaScriptDslConsumer.class, JavaScriptDslFunction.class,
                        JavaScriptDslPredicate.class, org.apache.camel.quarkus.dsl.js.runtime.JavaScriptDslProcessor.class,
                        JavaScriptDslSupplier.class));
    }

    private static String extractName(ClassInfo classInfo) {
        String className = classInfo.simpleName();
        int index = className.lastIndexOf('.');
        return className.substring(index + 1).replace("Component", "");
    }
}
