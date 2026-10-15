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
package org.apache.camel.quarkus.test.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.stub.StubComponent;
import org.apache.camel.impl.debugger.DefaultDebugger;
import org.apache.camel.model.Model;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.quarkus.test.AdviceRoute;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.apache.camel.quarkus.test.ProvidesBreakpoint;
import org.apache.camel.quarkus.test.ReplaceInRegistry;
import org.apache.camel.spi.AutowiredLifecycleStrategy;
import org.apache.camel.spi.Breakpoint;
import org.apache.camel.spi.CamelBeanPostProcessor;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.DefaultRegistry;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.test.junit6.StubComponentAutowireStrategy;
import org.apache.camel.test.junit6.util.CamelContextTestHelper;
import org.apache.camel.util.function.ThrowingRunnable;
import org.junit.platform.commons.support.HierarchyTraversalMode;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedFields;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedMethods;
import static org.junit.platform.commons.support.ReflectionSupport.invokeMethod;
import static org.junit.platform.commons.support.ReflectionSupport.newInstance;

/**
 * Applies and reverts the plan declared by {@link CamelQuarkusTest}.
 */
final class TestPlanSupport {

    private TestPlanSupport() {
        // Utility class
    }

    static void enableMocksAndStubs(CamelContext context, CamelQuarkusTest annotation) throws Exception {
        CamelContextTestHelper.enableAutoMocking(context, emptyToNull(annotation.mockEndpoints()),
                emptyToNull(annotation.mockEndpointsAndSkip()));
        if (!annotation.stubEndpoints().isEmpty()) {
            enableAutoStub(context, annotation.stubEndpoints());
        }
    }

    /**
     * Same as {@link CamelContextTestHelper#enableAutoStub(CamelContext, String)}, but delegating to the component
     * resolver of Camel Quarkus, which only knows the components discovered at build time.
     */
    private static void enableAutoStub(CamelContext context, String componentNames) {
        Set<String> stubbed = Arrays.stream(componentNames.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());

        StubComponent stub = context.getComponent("stub", StubComponent.class);
        stub.setShadow(true);
        stub.setShadowPattern(componentNames);
        stub.setAutowiredEnabled(false);

        ComponentResolver delegate = PluginHelper.getComponentResolver(context);
        context.getCamelContextExtension().addContextPlugin(ComponentResolver.class,
                (name, camelContext) -> stubbed.contains(name) ? new StubComponent() : delegate.resolveComponent(name,
                        camelContext));
        context.getLifecycleStrategies()
                .removeIf(AutowiredLifecycleStrategy.class::isInstance);
        context.getLifecycleStrategies().add(new StubComponentAutowireStrategy(context, componentNames));
    }

    static void enableDebugger(CamelContext context, Class<?> testClass) {
        List<Breakpoint> breakpoints = new ArrayList<>();
        for (Method method : findAnnotatedMethods(testClass, ProvidesBreakpoint.class, HierarchyTraversalMode.BOTTOM_UP)) {
            if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0
                    || !Breakpoint.class.isAssignableFrom(method.getReturnType())) {
                throw new IllegalStateException(String.format(
                        "@ProvidesBreakpoint method %s.%s must be static, take no parameters and return %s",
                        testClass.getName(), method.getName(), Breakpoint.class.getName()));
            }
            breakpoints.add((Breakpoint) invokeMethod(method, null));
        }
        if (!breakpoints.isEmpty()) {
            context.setDebugging(true);
            DefaultDebugger debugger = new DefaultDebugger();
            context.setDebugger(debugger);
            breakpoints.forEach(debugger::addBreakpoint);
        }
    }

    /**
     * Adds the routes of {@link CamelQuarkusTest#routes()} and applies {@link CamelQuarkusTest#replaceRouteFromWith()}
     * and {@link CamelQuarkusTest#advices()}.
     */
    static void applyRoutesAndAdvice(CamelContext context, TestClassState state) throws Exception {
        Model model = context.getCamelContextExtension().getContextPlugin(Model.class);
        CamelBeanPostProcessor beanPostProcessor = PluginHelper.getBeanPostProcessor(context);

        for (Class<? extends RoutesBuilder> routesClass : state.annotation.routes()) {
            List<RouteDefinition> before = new ArrayList<>(model.getRouteDefinitions());
            RoutesBuilder builder = newInstance(routesClass);
            beanPostProcessor.postProcessBeforeInitialization(builder, routesClass.getName());
            beanPostProcessor.postProcessAfterInitialization(builder, routesClass.getName());
            context.addRoutes(builder);
            for (RouteDefinition definition : model.getRouteDefinitions()) {
                if (before.stream().noneMatch(existing -> existing == definition)) {
                    state.addedRoutes.add(definition);
                }
            }
        }

        for (String replaceFrom : state.annotation.replaceRouteFromWith()) {
            int index = replaceFrom.indexOf('=');
            if (index < 1) {
                throw new IllegalStateException(String.format(
                        "@CamelQuarkusTest(replaceRouteFromWith) entry '%s' on %s must have the format routeId=uri",
                        replaceFrom, state.testClass.getName()));
            }
            String routeId = replaceFrom.substring(0, index);
            String uri = replaceFrom.substring(index + 1);
            AdviceWith.adviceWith(routeDefinition(model, routeId, state), context, new AdviceWithRouteBuilder() {
                @Override
                public void configure() {
                    replaceFromWith(uri);
                }
            });
        }

        for (AdviceRoute adviceRoute : state.annotation.advices()) {
            AdviceWithRouteBuilder advice = newInstance(adviceRoute.advice());
            beanPostProcessor.postProcessBeforeInitialization(advice, adviceRoute.advice().getName());
            beanPostProcessor.postProcessAfterInitialization(advice, adviceRoute.advice().getName());
            AdviceWith.adviceWith(routeDefinition(model, adviceRoute.route(), state), context, advice);
        }
    }

    /**
     * Binds the {@link ReplaceInRegistry} members of the given test instance, remembering the previous bindings.
     */
    static void replaceInRegistry(CamelContext context, TestClassState state, Object testInstance) {
        Registry registry = context.getRegistry();
        Class<?> testClass = testInstance.getClass();
        for (Method method : findAnnotatedMethods(testClass, ReplaceInRegistry.class, HierarchyTraversalMode.BOTTOM_UP)) {
            if (method.getParameterCount() != 0) {
                throw new IllegalStateException(String.format("@ReplaceInRegistry method %s.%s must not take parameters",
                        testClass.getName(), method.getName()));
            }
            bind(registry, state, method.getName(), method.getReturnType(), invokeMethod(method, testInstance));
        }
        for (Field field : findAnnotatedFields(testClass, ReplaceInRegistry.class)) {
            if (Modifier.isStatic(field.getModifiers())) {
                throw new IllegalStateException(String.format("@ReplaceInRegistry field %s.%s must not be static",
                        testClass.getName(), field.getName()));
            }
            try {
                field.setAccessible(true);
                bind(registry, state, field.getName(), field.getType(), field.get(testInstance));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Reverts the class plan: removes the added routes, restores advised routes and registry bindings, and removes
     * components created while the class ran.
     */
    static void revert(CamelContext context, TestClassState state) throws Exception {
        Model model = context.getCamelContextExtension().getContextPlugin(Model.class);
        for (RouteDefinition definition : state.addedRoutes) {
            model.removeRouteDefinition(definition);
        }

        if (state.classSnapshot != null) {
            state.classSnapshot.restoreAndReload(context);
        }

        Registry registry = context.getRegistry();
        for (Map.Entry<String, Optional<Object>> entry : state.replacedRegistryBeans.entrySet()) {
            registry.unbind(entry.getKey());
            entry.getValue().ifPresent(previous -> registry.bind(entry.getKey(), previous));
        }

        if (state.autoStartupExcludeReplaced) {
            context.setAutoStartupExcludePattern(state.previousAutoStartupExclude);
        }
        if (state.previousShutdownTimeout != null) {
            context.getShutdownStrategy().setTimeout(state.previousShutdownTimeout);
        }

        if (state.existingComponents != null) {
            TestContextCleanup.removeComponentsNotIn(context, state.existingComponents);
        }
        MockEndpoint.resetMocks(context);
    }

    private static void bind(Registry registry, TestClassState state, String name, Class<?> type, Object bean) {
        if (!state.replacedRegistryBeans.containsKey(name)) {
            Object previous = registry instanceof DefaultRegistry defaultRegistry
                    ? defaultRegistry.getFallbackRegistry().lookupByName(name)
                    : null;
            state.replacedRegistryBeans.put(name, Optional.ofNullable(previous));
        }
        registry.bind(name, type, bean);
        if (bean != null && registry.lookupByNameAndType(name, type) != bean) {
            throw new IllegalStateException(String.format(
                    "@ReplaceInRegistry %s.%s cannot replace bean '%s' because it is provided by CDI. Use @InjectMock or QuarkusMock instead",
                    state.testClass.getName(), name, name));
        }
    }

    private static RouteDefinition routeDefinition(Model model, String routeId, TestClassState state) {
        RouteDefinition definition = model.getRouteDefinition(routeId);
        if (definition == null) {
            throw new IllegalStateException(String.format("%s: route '%s' does not exist", state.testClass.getName(), routeId));
        }
        return definition;
    }

    static void unchecked(ThrowingRunnable<Exception> action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
