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
package org.apache.camel.quarkus.component.groovy.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.language.groovy.GroovyLanguage;
import org.apache.camel.quarkus.component.groovy.runtime.GroovyExpressionRecorder;
import org.apache.camel.quarkus.component.groovy.runtime.GroovyStaticScript;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.support.language.deployment.ExpressionBuildItem;
import org.apache.camel.quarkus.support.language.deployment.ExpressionExtractionResultBuildItem;
import org.apache.camel.quarkus.support.language.deployment.ScriptBuildItem;
import org.apache.camel.quarkus.support.language.runtime.ExpressionUID;
import org.apache.camel.quarkus.support.language.runtime.ScriptUID;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.tools.GroovyClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GroovyProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(GroovyProcessor.class);

    private static final String PACKAGE_NAME = "org.apache.camel.quarkus.component.groovy.generated";
    private static final String SCRIPT_FORMAT = """
            package %s
            @groovy.transform.CompileStatic
            class %s extends %s {
              Object run() {
                %s
              }
            }
            """;
    private static final String FEATURE = "camel-groovy";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void collectExpressions(ExpressionExtractionResultBuildItem result,
            List<ExpressionBuildItem> expressions,
            List<ScriptBuildItem> scripts,
            BuildProducer<GroovyExpressionSourceBuildItem> producer) {
        if (result.isSuccess()) {
            List<ExpressionBuildItem> groovyExpressions = expressions.stream()
                    .filter(exp -> "groovy".equals(exp.getLanguage()))
                    .toList();
            List<ScriptBuildItem> groovyScripts = scripts.stream()
                    .filter(exp -> "groovy".equals(exp.getLanguage()))
                    .toList();
            if (groovyExpressions.isEmpty() && groovyScripts.isEmpty()) {
                return;
            }
            for (ExpressionBuildItem expression : groovyExpressions) {
                String original = expression.getExpression();
                ExpressionUID id = new ExpressionUID(original);
                String name = String.format("%s.%s", PACKAGE_NAME, id);
                String content = toScriptClass(id.asJavaIdentifier(), expression.getLoadedExpression());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Extracting expression:\n\n{}\n", content);
                }
                producer.produce(new GroovyExpressionSourceBuildItem(name, original, content));
            }
            for (ScriptBuildItem script : groovyScripts) {
                String original = script.getContent();
                ScriptUID id = new ScriptUID(original, script.getBindings());
                String name = String.format("%s.%s", PACKAGE_NAME, id);
                String content = toScriptClass(id.asJavaIdentifier(), script.getLoadedContent());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Extracting script:\n\n{}\n", content);
                }
                producer.produce(new GroovyExpressionSourceBuildItem(name, original, content));
            }
        }
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void compileScriptsAOT(CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<GroovyExpressionSourceBuildItem> sources,
            BuildProducer<GeneratedClassBuildItem> generatedClass) {
        if (sources.isEmpty()) {
            return;
        }
        CompilationUnit unit = new CompilationUnit();
        Set<String> classNames = new HashSet<>();
        for (GroovyExpressionSourceBuildItem source : sources) {
            String name = source.getClassName();
            String content = source.getSourceCode();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Compiling script:\n\n{}\n", content);
            }
            unit.addSource(name, content);
            classNames.add(name);
        }
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setClasspathList(
                curateOutcomeBuildItem.getApplicationModel().getDependencies().stream()
                        .map(ResolvedDependency::getResolvedPaths)
                        .flatMap(PathCollection::stream)
                        .map(Objects::toString)
                        .toList());
        unit.configure(cc);
        unit.compile(Phases.CLASS_GENERATION);
        for (GroovyClass clazz : unit.getClasses()) {
            String className = clazz.getName();
            generatedClass.produce(new GeneratedClassBuildItem(true, className, clazz.getBytes()));
            if (classNames.contains(className)) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(className).methods().fields().build());
            }
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = NativeBuild.class)
    CamelBeanBuildItem configureLanguage(
            RecorderContext recorderContext,
            GroovyExpressionRecorder recorder,
            ExpressionExtractionResultBuildItem result,
            List<GroovyExpressionSourceBuildItem> sources) {
        if (result.isSuccess() && !sources.isEmpty()) {
            RuntimeValue<GroovyLanguage.Builder> builder = recorder.languageBuilder();
            for (GroovyExpressionSourceBuildItem source : sources) {
                recorder.addScript(builder, source.getOriginalCode(), recorderContext.classProxy(source.getClassName()));
            }
            final RuntimeValue<GroovyLanguage> language = recorder.languageNewInstance(builder);
            return new CamelBeanBuildItem("groovy", GroovyLanguage.class.getName(), language);
        }
        return null;
    }

    /**
     * Convert a Groovy expression into a Script class to be able to compile it.
     *
     * @param  name          the name of the Groovy expression
     * @param  contentScript the content of the Groovy script
     * @return               the content of the corresponding Script class.
     */
    private static String toScriptClass(String name, String contentScript) {
        return String.format(SCRIPT_FORMAT, PACKAGE_NAME, name, GroovyStaticScript.class.getName(), contentScript);
    }
}
