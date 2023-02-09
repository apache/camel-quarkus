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
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.dsl.java.joor.CompilationUnit;
import org.apache.camel.dsl.java.joor.Helper;
import org.apache.camel.dsl.java.joor.MultiCompile;
import org.apache.camel.quarkus.core.deployment.main.CamelMainHelper;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.dsl.java.joor.runtime.JavaJoorDslRecorder;
import org.apache.camel.spi.Resource;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaJoorDslProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(JavaJoorDslProcessor.class);
    private static final String FEATURE = "camel-java-joor-dsl";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void compileClassesAOT(BuildProducer<JavaJoorGeneratedClassBuildItem> generatedClass,
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
                        String name = Helper.determineName(resource, content);
                        unit.addClass(name, content);
                        nameToResource.put(name, resource);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        if (nameToResource.isEmpty()) {
            return;
        }
        LOG.debug("Compiling unit: {}", unit);
        CompilationUnit.Result result = MultiCompile.compileUnit(
                unit,
                List.of(
                        "-classpath",
                        curateOutcomeBuildItem.getApplicationModel().getDependencies().stream()
                                .map(ResolvedDependency::getResolvedPaths)
                                .flatMap(PathCollection::stream)
                                .map(Objects::toString)
                                .collect(Collectors.joining(System.getProperty("path.separator")))));
        for (String className : result.getClassNames()) {
            generatedClass
                    .produce(new JavaJoorGeneratedClassBuildItem(className, nameToResource.get(className).getLocation(),
                            result.getByteCode(className)));
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
}
