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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * Inspects the test classes index for {@code @CamelQuarkusTest} classes and validates their use of the annotation.
 * <p>
 * Works on class names only, so it can run at build time without loading test classes.
 */
public final class CamelQuarkusTestInspector {
    static final DotName CAMEL_QUARKUS_TEST = DotName
            .createSimple("org.apache.camel.quarkus.test.CamelQuarkusTest");
    static final DotName ADVICE_ROUTE = DotName.createSimple("org.apache.camel.quarkus.test.AdviceRoute");
    static final DotName REPLACE_IN_REGISTRY = DotName.createSimple("org.apache.camel.quarkus.test.ReplaceInRegistry");
    static final DotName PROVIDES_BREAKPOINT = DotName.createSimple("org.apache.camel.quarkus.test.ProvidesBreakpoint");
    static final DotName CAMEL_QUARKUS_TEST_SUPPORT = DotName
            .createSimple("org.apache.camel.quarkus.test.CamelQuarkusTestSupport");
    static final DotName TEST_PROFILE = DotName.createSimple("io.quarkus.test.junit.TestProfile");
    static final DotName QUARKUS_INTEGRATION_TEST = DotName
            .createSimple("io.quarkus.test.junit.QuarkusIntegrationTest");
    static final DotName NESTED = DotName.createSimple("org.junit.jupiter.api.Nested");
    static final DotName BREAKPOINT = DotName.createSimple("org.apache.camel.spi.Breakpoint");

    static final String MANAGEMENT_MARKER = "org.apache.camel.quarkus.component.management.CamelManagementRecorder";
    static final String XML_IO_DSL_MARKER = "org.apache.camel.quarkus.dsl.xml.XmlIoDslRecorder";
    static final String XML_JAXB_MARKER = "org.apache.camel.quarkus.component.xml.jaxb.XmlJaxbRecorder";
    static final String YAML_IO_MARKER = "org.apache.camel.quarkus.yaml.io.YamlIORecorder";
    static final String DIAGRAM_MARKER = "org.apache.camel.quarkus.component.diagram.CamelDiagramRecorder";

    private static final List<String> ISOLATED_ONLY_ATTRIBUTES = List.of("mockEndpoints", "mockEndpointsAndSkip",
            "stubEndpoints", "autoStartupExclude");

    private CamelQuarkusTestInspector() {
        // Utility class
    }

    /**
     * The outcome of inspecting the test classes index.
     *
     * @param testClasses           the concrete classes using {@code @CamelQuarkusTest}, directly or inherited
     * @param routesBuilderExcludes path patterns of {@code RoutesBuilder} classes to exclude from route discovery
     * @param isolatedTestClasses   test profile class name to the single {@code @CamelQuarkusTest} class using it
     * @param errors                validation errors, empty if the annotation is used correctly
     */
    public record Result(
            Set<String> testClasses,
            Set<String> routesBuilderExcludes,
            Map<String, String> isolatedTestClasses,
            List<String> errors) {

        public boolean isEmpty() {
            return testClasses.isEmpty();
        }

        /**
         * @throws IllegalStateException listing all errors, if there are any
         */
        public void assertValid() {
            if (!errors.isEmpty()) {
                throw new IllegalStateException("Invalid use of @CamelQuarkusTest:\n  - " + String.join("\n  - ", errors));
            }
        }
    }

    /**
     * Inspects the given test classes index.
     *
     * @param  index        the test classes index
     * @param  classPresent tells whether a class is present on the application class path
     * @return              the inspection result
     */
    public static Result inspect(IndexView index, Predicate<String> classPresent) {
        List<String> errors = new ArrayList<>();
        Set<String> routesBuilderExcludes = new LinkedHashSet<>();
        Map<String, AnnotationInstance> annotationByTestClass = new TreeMap<>();
        Set<DotName> membersValidated = new HashSet<>();

        for (AnnotationInstance annotation : index.getAnnotations(CAMEL_QUARKUS_TEST)) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo annotated = annotation.target().asClass();
            if (annotated.isAnnotation()) {
                continue;
            }
            validateAnnotatedClass(index, annotated, annotation, classPresent, errors);
            validateMembers(index, annotated, membersValidated, errors);
            routesBuilderExcludes.add(annotated.name().toString().replace('.', '/') + "$*");
            for (Type routes : classArray(annotation, "routes")) {
                routesBuilderExcludes.add(routes.name().toString().replace('.', '/'));
            }

            if (!Modifier.isAbstract(annotated.flags())) {
                annotationByTestClass.put(annotated.name().toString(), annotation);
            }
            for (ClassInfo subclass : index.getAllKnownSubclasses(annotated.name())) {
                if (subclass.hasDeclaredAnnotation(CAMEL_QUARKUS_TEST)
                        || subclass.hasDeclaredAnnotation(QUARKUS_INTEGRATION_TEST)) {
                    continue;
                }
                validateMembers(index, subclass, membersValidated, errors);
                routesBuilderExcludes.add(subclass.name().toString().replace('.', '/') + "$*");
                if (!Modifier.isAbstract(subclass.flags())) {
                    annotationByTestClass.putIfAbsent(subclass.name().toString(), nearestAnnotation(index, subclass));
                }
            }
        }

        // Group the test classes by test profile to find out which ones own their application
        Map<String, List<String>> testClassesByProfile = new LinkedHashMap<>();
        for (String testClass : annotationByTestClass.keySet()) {
            testProfile(index, index.getClassByName(testClass))
                    .ifPresent(profile -> testClassesByProfile.computeIfAbsent(profile, p -> new ArrayList<>()).add(testClass));
        }
        Map<String, String> isolatedTestClasses = new LinkedHashMap<>();
        testClassesByProfile.forEach((profile, testClasses) -> {
            if (testClasses.size() == 1) {
                isolatedTestClasses.put(profile, testClasses.get(0));
            }
        });

        for (Map.Entry<String, AnnotationInstance> entry : annotationByTestClass.entrySet()) {
            String testClass = entry.getKey();
            if (isolatedTestClasses.containsValue(testClass)) {
                continue;
            }
            List<String> isolatedOnly = isolatedOnlyFeatures(index, index.getClassByName(testClass), entry.getValue());
            if (!isolatedOnly.isEmpty()) {
                Optional<String> profile = testProfile(index, index.getClassByName(testClass));
                String reason = profile.isPresent()
                        ? String.format("its test profile %s is also used by %s", profile.get(),
                                String.join(", ", without(testClassesByProfile.get(profile.get()), testClass)))
                        : "it has no test profile";
                errors.add(String.format(
                        "%s uses %s, which need the class to own its application, but %s. Annotate it with @TestProfile(%s.class) and make it a public class implementing QuarkusTestProfile, or use a test profile used by no other @CamelQuarkusTest class",
                        testClass, String.join(", ", isolatedOnly), reason, simpleName(testClass)));
            }
        }

        return new Result(annotationByTestClass.keySet(), routesBuilderExcludes, isolatedTestClasses, errors);
    }

    private static void validateAnnotatedClass(IndexView index, ClassInfo annotated, AnnotationInstance annotation,
            Predicate<String> classPresent, List<String> errors) {
        String name = annotated.name().toString();

        if (annotated.hasDeclaredAnnotation(QUARKUS_INTEGRATION_TEST)) {
            errors.add(name + " is a @QuarkusIntegrationTest. @CamelQuarkusTest only supports JVM mode tests");
        }
        if (annotated.hasDeclaredAnnotation(NESTED)) {
            errors.add(name + " is a @Nested class. Declare @CamelQuarkusTest on the enclosing class");
        }
        if (extendsClass(index, annotated, CAMEL_QUARKUS_TEST_SUPPORT)) {
            errors.add(name + " extends CamelQuarkusTestSupport, which cannot be combined with @CamelQuarkusTest");
        }

        for (String replaceFrom : stringArray(annotation, "replaceRouteFromWith")) {
            if (replaceFrom.indexOf('=') < 1 || replaceFrom.indexOf('=') == replaceFrom.length() - 1) {
                errors.add(String.format("%s: replaceRouteFromWith entry '%s' must have the format routeId=uri", name,
                        replaceFrom));
            }
        }

        for (Type routes : classArray(annotation, "routes")) {
            validateInstantiable(index, routes.name(), name + ": routes class", errors);
        }

        AnnotationValue advices = annotation.value("advices");
        if (advices != null) {
            for (AnnotationInstance advice : advices.asNestedArray()) {
                validateInstantiable(index, advice.value("advice").asClass().name(), name + ": advice class", errors);
                if (advice.value("route").asString().isBlank()) {
                    errors.add(name + ": @AdviceRoute route must not be blank");
                }
            }
        }

        AnnotationValue dumpRouteCoverage = annotation.value("dumpRouteCoverage");
        if (dumpRouteCoverage != null && dumpRouteCoverage.asBoolean() && !classPresent.test(MANAGEMENT_MARKER)) {
            errors.add(name + ": dumpRouteCoverage requires the camel-quarkus-management dependency");
        }
        AnnotationValue dumpRoute = annotation.value("dumpRoute");
        if (dumpRoute != null && !dumpRoute.asString().isEmpty()) {
            switch (dumpRoute.asString()) {
            case "xml" -> {
                if (!classPresent.test(XML_IO_DSL_MARKER) && !classPresent.test(XML_JAXB_MARKER)) {
                    errors.add(name + ": dumpRoute = \"xml\" requires the camel-quarkus-xml-io-dsl dependency");
                }
            }
            case "yaml" -> {
                if (!classPresent.test(YAML_IO_MARKER)) {
                    errors.add(name + ": dumpRoute = \"yaml\" requires the camel-quarkus-yaml-io dependency");
                }
            }
            default -> errors.add(String.format("%s: dumpRoute must be \"xml\" or \"yaml\", not \"%s\"", name,
                    dumpRoute.asString()));
            }
        }
        AnnotationValue dumpRouteDiagramFolder = annotation.value("dumpRouteDiagramFolder");
        if (dumpRouteDiagramFolder != null && !dumpRouteDiagramFolder.asString().isBlank()) {
            if (!classPresent.test(DIAGRAM_MARKER)) {
                errors.add(name + ": dumpRouteDiagramFolder requires the camel-quarkus-diagram dependency");
            }
        } else {
            for (String option : List.of("dumpRouteDiagramTopology", "dumpRouteDiagramTopologyExternal")) {
                if (annotation.value(option) != null) {
                    errors.add(name + ": " + option + " requires dumpRouteDiagramFolder");
                }
            }
        }
    }

    /**
     * Validates the annotated members of a class and its superclasses, skipping the classes already validated.
     */
    private static void validateMembers(IndexView index, ClassInfo classInfo, Set<DotName> validated, List<String> errors) {
        for (ClassInfo current = classInfo; current != null
                && validated.add(current.name()); current = index.getClassByName(current.superName())) {
            String name = current.name().toString();
            for (MethodInfo method : current.methods()) {
                if (method.hasDeclaredAnnotation(PROVIDES_BREAKPOINT)
                        && (!Modifier.isPublic(method.flags()) || !Modifier.isStatic(method.flags())
                                || method.parametersCount() != 0 || !BREAKPOINT.equals(method.returnType().name()))) {
                    errors.add(String.format(
                            "%s: @ProvidesBreakpoint method %s must be public static, take no parameters and return org.apache.camel.spi.Breakpoint",
                            name, method.name()));
                }
                if (method.hasDeclaredAnnotation(REPLACE_IN_REGISTRY)
                        && (method.parametersCount() != 0 || method.returnType().kind() == Type.Kind.VOID)) {
                    errors.add(String.format("%s: @ReplaceInRegistry method %s must take no parameters and return a value",
                            name, method.name()));
                }
            }
            for (FieldInfo field : current.fields()) {
                if (field.hasDeclaredAnnotation(REPLACE_IN_REGISTRY) && Modifier.isStatic(field.flags())) {
                    errors.add(String.format("%s: @ReplaceInRegistry field %s must not be static", name, field.name()));
                }
            }
        }
    }

    private static List<String> isolatedOnlyFeatures(IndexView index, ClassInfo testClass, AnnotationInstance annotation) {
        List<String> features = new ArrayList<>();
        for (String attribute : ISOLATED_ONLY_ATTRIBUTES) {
            AnnotationValue value = annotation.value(attribute);
            if (value != null && !value.asString().isEmpty()) {
                features.add(attribute);
            }
        }
        if (!annotatedMethods(index, testClass, PROVIDES_BREAKPOINT).isEmpty()) {
            features.add("@ProvidesBreakpoint");
        }
        return features;
    }

    private static void validateInstantiable(IndexView index, DotName className, String description, List<String> errors) {
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            // Not a test class, the compiler and the runtime check the rest
            return;
        }
        if (Modifier.isAbstract(classInfo.flags())) {
            errors.add(String.format("%s %s must not be abstract", description, className));
        } else if (classInfo.method("<init>") == null) {
            // Also the case for inner classes, whose constructors take the enclosing instance
            errors.add(String.format("%s %s must have a no-arg constructor and be static if nested", description,
                    className));
        }
    }

    private static AnnotationInstance nearestAnnotation(IndexView index, ClassInfo classInfo) {
        ClassInfo current = classInfo;
        while (current != null) {
            AnnotationInstance annotation = current.declaredAnnotation(CAMEL_QUARKUS_TEST);
            if (annotation != null) {
                return annotation;
            }
            current = index.getClassByName(current.superName());
        }
        throw new IllegalStateException("No @CamelQuarkusTest found on " + classInfo.name() + " or its superclasses");
    }

    /**
     * The test profile of a class, resolved the way Quarkus does: on the class itself, then on its enclosing classes.
     */
    private static Optional<String> testProfile(IndexView index, ClassInfo classInfo) {
        ClassInfo current = classInfo;
        while (current != null) {
            AnnotationInstance testProfile = current.declaredAnnotation(TEST_PROFILE);
            if (testProfile != null) {
                return Optional.of(testProfile.value().asClass().name().toString());
            }
            DotName enclosing = current.enclosingClass();
            current = enclosing != null ? index.getClassByName(enclosing) : null;
        }
        return Optional.empty();
    }

    private static boolean extendsClass(IndexView index, ClassInfo classInfo, DotName superClass) {
        ClassInfo current = classInfo;
        while (current != null && current.superName() != null) {
            if (current.superName().equals(superClass)) {
                return true;
            }
            current = index.getClassByName(current.superName());
        }
        return false;
    }

    private static List<MethodInfo> annotatedMethods(IndexView index, ClassInfo classInfo, DotName annotation) {
        List<MethodInfo> methods = new ArrayList<>();
        for (ClassInfo current = classInfo; current != null; current = index.getClassByName(current.superName())) {
            for (MethodInfo method : current.methods()) {
                if (method.hasDeclaredAnnotation(annotation)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private static Type[] classArray(AnnotationInstance annotation, String name) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asClassArray() : new Type[0];
    }

    private static String[] stringArray(AnnotationInstance annotation, String name) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asStringArray() : new String[0];
    }

    private static List<String> without(Collection<String> values, String value) {
        return values.stream().filter(v -> !v.equals(value)).toList();
    }

    private static String simpleName(String className) {
        return className.substring(Math.max(className.lastIndexOf('.'), className.lastIndexOf('$')) + 1);
    }
}
