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

package org.apache.camel.quarkus.dsl.java.joor.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.LambdaCapturingTypeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.CamelContext;
import org.apache.camel.dsl.java.joor.CompilationUnit;
import org.apache.camel.dsl.java.joor.MultiCompile;
import org.apache.camel.quarkus.core.deployment.main.CamelMainHelper;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.dsl.java.joor.runtime.JavaJoorDslRecorder;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaJoorDslProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(JavaJoorDslProcessor.class);
    private static final String FEATURE = "camel-java-joor-dsl";

    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "^\\s*package\\s+([a-zA-Z][\\.\\w]*)\\s*;.*$", Pattern.MULTILINE);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void compileClassesAOT(BuildProducer<JavaJoorGeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<LambdaCapturingTypeBuildItem> lambdaCapturingTypeProducer,
            CurateOutcomeBuildItem curateOutcomeBuildItem) throws Exception {
        Map<String, Resource> nameToResource = new HashMap<>();
        LOG.debug("Loading .java resources");
        CompilationUnit unit = CompilationUnit.input();
        CamelMainHelper.forEachMatchingResource(
                resource -> {
                    if (!resource.getLocation().endsWith(".java")) {
                        return;
                    }
                    try (InputStream is = resource.getInputStream()) {
                        String content = IOHelper.loadText(is);
                        String name = determineName(resource, content);
                        unit.addClass(name, content);
                        nameToResource.put(name, resource);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        if (nameToResource.isEmpty()) {
            return;
        }
        // Temporary workaround until being able to provide the classpath somehow to the method MultiCompile.compileUnit
        // https://issues.apache.org/jira/browse/CAMEL-18860
        String previousCP = System.getProperty("java.class.path");
        try {
            String cp = curateOutcomeBuildItem.getApplicationModel().getDependencies().stream()
                    .map(ResolvedDependency::getResolvedPaths)
                    .flatMap(PathCollection::stream)
                    .map(Objects::toString)
                    .collect(Collectors.joining(System.getProperty("path.separator")));
            System.setProperty("java.class.path", cp);
            LOG.debug("Compiling unit: {}", unit);
            CompilationUnit.Result result = MultiCompile.compileUnit(unit);
            for (String className : result.getClassNames()) {
                generatedClass
                        .produce(new JavaJoorGeneratedClassBuildItem(className, nameToResource.get(className).getLocation(),
                                result.getByteCode(className)));
                registerForReflection(reflectiveClass, lambdaCapturingTypeProducer,
                        result.getClass(className).getAnnotation(RegisterForReflection.class));
            }
        } finally {
            // Restore the CP
            System.setProperty("java.class.path", previousCP);
        }
    }

    private void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<LambdaCapturingTypeBuildItem> lambdaCapturingTypeProducer,
            RegisterForReflection annotation) {
        if (annotation == null) {
            return;
        }

        for (String lambdaCapturingType : annotation.lambdaCapturingTypes()) {
            lambdaCapturingTypeProducer.produce(new LambdaCapturingTypeBuildItem(lambdaCapturingType));
        }
        boolean methods = annotation.methods();
        boolean fields = annotation.fields();
        boolean ignoreNested = annotation.ignoreNested();
        boolean serialization = annotation.serialization();

        if (annotation.registerFullHierarchy()) {
            LOG.warn(
                    "The element 'registerFullHierarchy' of the annotation 'RegisterForReflection' is not supported by the extension Camel Java jOOR DSL");
        }
        for (Class<?> type : annotation.targets()) {
            registerClass(type.getName(), methods, fields, ignoreNested, serialization, reflectiveClass);
        }

        for (String className : annotation.classNames()) {
            registerClass(className, methods, fields, ignoreNested, serialization, reflectiveClass);
        }
    }

    private void registerClass(String className, boolean methods, boolean fields, boolean ignoreNested, boolean serialization,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(serialization
                ? ReflectiveClassBuildItem.serializationClass(className)
                : new ReflectiveClassBuildItem(methods, fields, className));

        if (ignoreNested) {
            return;
        }

        try {
            Class<?>[] declaredClasses = Thread.currentThread().getContextClassLoader().loadClass(className)
                    .getDeclaredClasses();
            for (Class<?> clazz : declaredClasses) {
                registerClass(clazz.getName(), methods, fields, false, serialization, reflectiveClass);
            }
        } catch (ClassNotFoundException e) {
            LOG.warn("Failed to load Class {}", className, e);
        }
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void registerGeneratedClasses(BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<JavaJoorGeneratedClassBuildItem> classes) {

        for (JavaJoorGeneratedClassBuildItem clazz : classes) {
            generatedClass.produce(new GeneratedClassBuildItem(true, clazz.getName(), clazz.getClassData()));
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, clazz.getName()));
        }
    }

    @BuildStep(onlyIf = NativeBuild.class)
    @Record(value = ExecutionTime.STATIC_INIT)
    void registerRoutesBuilder(List<JavaJoorGeneratedClassBuildItem> classes,
            CamelContextBuildItem context,
            JavaJoorDslRecorder recorder) throws Exception {
        RuntimeValue<CamelContext> camelContext = context.getCamelContext();
        for (JavaJoorGeneratedClassBuildItem clazz : classes) {
            recorder.registerRoutesBuilder(camelContext, clazz.getName(), clazz.getLocation());
        }
    }

    private static String determineName(Resource resource, String content) {
        String loc = resource.getLocation();
        // strip scheme to compute the name
        String scheme = ResourceHelper.getScheme(loc);
        if (scheme != null) {
            loc = loc.substring(scheme.length());
        }
        final String name = FileUtil.onlyName(loc, true);
        final Matcher matcher = PACKAGE_PATTERN.matcher(content);

        return matcher.find()
                ? matcher.group(1) + "." + name
                : name;
    }
}
