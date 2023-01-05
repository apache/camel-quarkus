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

package org.apache.camel.quarkus.dsl.groovy.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.core.deployment.main.CamelMainHelper;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.dsl.groovy.runtime.Configurer;
import org.apache.camel.quarkus.dsl.groovy.runtime.GroovyDslRecorder;
import org.apache.camel.spi.Resource;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.IOHelper;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.tools.GroovyClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroovyDslProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(GroovyDslProcessor.class);
    private static final String PACKAGE_NAME = "org.apache.camel.quarkus.dsl.groovy.generated";
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import .*");
    private static final String FILE_FORMAT = "package %s\n" +
            "%s\n" +
            "@groovy.transform.InheritConstructors \n" +
            "class %s extends %s {\n" +
            "  void configure(){ \n" +
            "    %s\n" +
            "  }\n" +
            "}";
    private static final String FEATURE = "camel-groovy-dsl";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void compileScriptsAOT(BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<GroovyGeneratedClassBuildItem> generatedGroovyClass,
            CurateOutcomeBuildItem curateOutcomeBuildItem) throws Exception {
        LOG.debug("Loading .groovy resources");
        Map<String, Resource> nameToResource = new HashMap<>();
        CompilationUnit unit = new CompilationUnit();
        CamelMainHelper.forEachMatchingResource(
                resource -> {
                    if (!resource.getLocation().endsWith(".groovy")) {
                        return;
                    }
                    try (InputStream is = resource.getInputStream()) {
                        String name = determineName(resource);
                        String fqn = String.format("%s.%s", PACKAGE_NAME, name);
                        unit.addSource(fqn, toGroovyClass(name, IOHelper.loadText(is)));
                        nameToResource.put(fqn, resource);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        if (nameToResource.isEmpty()) {
            return;
        }
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setClasspathList(
                curateOutcomeBuildItem.getApplicationModel().getDependencies().stream()
                        .map(ResolvedDependency::getResolvedPaths)
                        .flatMap(PathCollection::stream)
                        .map(Objects::toString)
                        .collect(Collectors.toList()));
        unit.configure(cc);
        unit.compile(Phases.CLASS_GENERATION);
        for (GroovyClass clazz : unit.getClasses()) {
            String className = clazz.getName();
            generatedClass.produce(new GeneratedClassBuildItem(true, className, clazz.getBytes()));
            if (nameToResource.containsKey(className)) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, className));
                generatedGroovyClass
                        .produce(new GroovyGeneratedClassBuildItem(className, nameToResource.get(className).getLocation()));
            }
        }
    }

    @BuildStep(onlyIf = NativeBuild.class)
    @Record(value = ExecutionTime.STATIC_INIT)
    void registerRoutesBuilder(List<GroovyGeneratedClassBuildItem> classes,
            CamelContextBuildItem context,
            GroovyDslRecorder recorder) throws Exception {
        RuntimeValue<CamelContext> camelContext = context.getCamelContext();
        for (GroovyGeneratedClassBuildItem clazz : classes) {
            recorder.registerRoutesBuilder(camelContext, clazz.getName(), clazz.getLocation());
        }
    }

    // To put it back once the Groovy extensions will be supported (https://github.com/apache/camel-quarkus/issues/4384)
    //    @BuildStep
    //    void registerNativeImageResources(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
    //        serviceProvider
    //                .produce(ServiceProviderBuildItem.allProvidersFromClassPath("org.codehaus.groovy.runtime.ExtensionModule"));
    //    }

    private static String determineName(Resource resource) {
        String str = FileUtil.onlyName(resource.getLocation(), true);
        StringBuilder sb = new StringBuilder();
        for (int i = 0, length = str.length(); i < length; i++) {
            char c = str.charAt(i);
            if ((i == 0 && Character.isJavaIdentifierStart(c)) || (i > 0 && Character.isJavaIdentifierPart(c))) {
                sb.append(c);
            } else {
                sb.append((int) c);
            }
        }
        return sb.toString();
    }

    /**
     * Convert a Groovy script into a Groovy class to be able to compile it.
     *
     * @param  name            the name of the Groovy class
     * @param  contentResource the content of the Groovy script
     * @return                 the content of the corresponding Groovy class.
     */
    private static String toGroovyClass(String name, String contentResource) {
        List<String> imports = new ArrayList<>();
        imports.add("import org.apache.camel.*");
        imports.add("import org.apache.camel.spi.*");
        Matcher m = IMPORT_PATTERN.matcher(contentResource);
        int beginIndex = 0;
        while (m.find()) {
            imports.add(m.group());
            beginIndex = m.end();
        }
        if (beginIndex > 0) {
            contentResource = contentResource.substring(beginIndex);
        }
        return String.format(
                FILE_FORMAT, PACKAGE_NAME, String.join("\n", imports), name, Configurer.class.getName(), contentResource);
    }
}
