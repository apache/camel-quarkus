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
package org.apache.camel.quarkus.component.joor.deployment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.dsl.java.joor.CompilationUnit;
import org.apache.camel.dsl.java.joor.MultiCompile;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.language.joor.JoorCompiler;
import org.apache.camel.language.joor.JoorExpression;
import org.apache.camel.language.joor.JoorScriptingCompiler;
import org.apache.camel.quarkus.component.joor.runtime.JoorExpressionCompiler;
import org.apache.camel.quarkus.component.joor.runtime.JoorExpressionConfig;
import org.apache.camel.quarkus.component.joor.runtime.JoorExpressionLanguage;
import org.apache.camel.quarkus.component.joor.runtime.JoorExpressionRecorder;
import org.apache.camel.quarkus.component.joor.runtime.JoorExpressionScriptingCompiler;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.support.language.deployment.ExpressionBuildItem;
import org.apache.camel.quarkus.support.language.deployment.ExpressionExtractionResultBuildItem;
import org.apache.camel.quarkus.support.language.deployment.ScriptBuildItem;
import org.apache.camel.quarkus.support.language.runtime.ExpressionUID;
import org.apache.camel.quarkus.support.language.runtime.ScriptUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.quarkus.component.joor.runtime.JoorExpressionLanguage.PACKAGE_NAME;

class JoorProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(JoorProcessor.class);
    private static final String FEATURE = "camel-joor";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void collectExpressions(JoorExpressionConfig config,
            ExpressionExtractionResultBuildItem result,
            List<ExpressionBuildItem> expressions,
            List<ScriptBuildItem> scripts,
            BuildProducer<JoorExpressionSourceBuildItem> producer) throws Exception {
        if (result.isSuccess()) {
            List<ExpressionBuildItem> joorExpressions = expressions.stream()
                    .filter(exp -> "joor".equals(exp.getLanguage()))
                    .collect(Collectors.toList());
            List<ScriptBuildItem> joorScripts = scripts.stream()
                    .filter(exp -> "joor".equals(exp.getLanguage()))
                    .collect(Collectors.toList());
            if (joorExpressions.isEmpty() && joorScripts.isEmpty()) {
                return;
            }
            // Don't close it as it won't be started and some log entries are added on close/stop
            CamelContext ctx = new DefaultCamelContext();
            try (JoorExpressionLanguage language = new JoorExpressionLanguage()) {
                language.setCamelContext(ctx);
                language.setSingleQuotes(config.singleQuotes);
                config.configResource.ifPresent(language::setConfigResource);
                language.setPreCompile(false);
                language.init();
                JoorCompiler compiler = language.getJoorCompiler();
                for (ExpressionBuildItem expression : joorExpressions) {
                    JoorExpression exp = (JoorExpression) language.createExpression(expression.getExpression(),
                            expression.getProperties());
                    ExpressionUID id = new ExpressionUID(expression.getExpression(), exp.isSingleQuotes());
                    String name = String.format("%s.%s", PACKAGE_NAME, id);
                    String content = evalCode(compiler, ctx, name, expression.getExpression(), exp.isSingleQuotes());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Compiling expression:\n\n{}\n", content);
                    }
                    producer.produce(new JoorExpressionSourceBuildItem(id, name, content));
                }
                JoorScriptingCompiler scriptingCompiler = language.getJoorScriptingCompiler();
                for (ScriptBuildItem script : joorScripts) {
                    ScriptUID id = new ScriptUID(script.getContent(), script.getBindings(), language.isSingleQuotes());
                    String name = String.format("%s.%s", PACKAGE_NAME, id);
                    String content = scriptingCompiler.evalCode(ctx, name, script.getContent(), script.getBindings(),
                            language.isSingleQuotes());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Compiling script:\n\n{}\n", content);
                    }
                    producer.produce(new JoorExpressionSourceBuildItem(id, name, content));
                }
            }
        }
    }

    @BuildStep
    void compileExpressions(CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<JoorExpressionSourceBuildItem> sources,
            BuildProducer<GeneratedClassBuildItem> generatedClass) {
        if (sources.isEmpty()) {
            return;
        }
        CompilationUnit unit = CompilationUnit.input();
        for (JoorExpressionSourceBuildItem source : sources) {
            unit.addClass(source.getClassName(), source.getSourceCode());
        }
        // Temporary workaround until being able to provide the classpath somehow to the method MultiCompile.compileUnit
        // https://issues.apache.org/jira/browse/CAMEL-18860
        String previousCP = System.getProperty("java.class.path");
        try {
            ApplicationModel model = curateOutcomeBuildItem.getApplicationModel();
            List<ResolvedDependency> dependencies = new ArrayList<>(model.getDependencies());
            dependencies.add(model.getAppArtifact());
            String cp = dependencies.stream()
                    .map(ResolvedDependency::getResolvedPaths)
                    .flatMap(PathCollection::stream)
                    .map(Objects::toString)
                    .collect(Collectors.joining(System.getProperty("path.separator")));
            System.setProperty("java.class.path", cp);
            LOG.debug("Compiling unit: {}", unit);
            CompilationUnit.Result compilationResult = MultiCompile.compileUnit(unit);
            for (String className : compilationResult.getClassNames()) {
                generatedClass
                        .produce(
                                new GeneratedClassBuildItem(true, className, compilationResult.getByteCode(className)));
            }
        } finally {
            // Restore the CP
            System.setProperty("java.class.path", previousCP);
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    @Consume(CamelContextBuildItem.class)
    CamelBeanBuildItem configureLanguage(
            JoorExpressionConfig config,
            RecorderContext recorderContext,
            JoorExpressionRecorder recorder,
            CamelContextBuildItem context,
            ExpressionExtractionResultBuildItem result,
            List<JoorExpressionSourceBuildItem> sources) {

        if (result.isSuccess() && !sources.isEmpty()) {
            final RuntimeValue<JoorExpressionCompiler.Builder> expressionCompilerBuilder = recorder
                    .expressionCompilerBuilder();
            final RuntimeValue<JoorExpressionScriptingCompiler.Builder> expressionScriptingCompilerBuilder = recorder
                    .expressionScriptingCompilerBuilder();
            RuntimeValue<CamelContext> camelContext = context.getCamelContext();
            for (JoorExpressionSourceBuildItem source : sources) {
                if (source.isScript()) {
                    recorder.addScript(expressionScriptingCompilerBuilder, camelContext, source.getId(),
                            recorderContext.classProxy(source.getClassName()));
                } else {
                    recorder.addExpression(expressionCompilerBuilder, camelContext, source.getId(),
                            recorderContext.classProxy(source.getClassName()));
                }
            }
            final RuntimeValue<JoorExpressionLanguage> language = recorder.languageNewInstance(config);
            config.resultType.ifPresent(c -> recorder.setResultType(language, recorderContext.classProxy(c)));
            recorder.setJoorCompiler(language, expressionCompilerBuilder);
            recorder.setJoorScriptingCompiler(language, expressionScriptingCompilerBuilder);
            return new CamelBeanBuildItem("joor", JoorExpressionLanguage.class.getName(), language);
        }
        return null;
    }

    /**
     * @return the code generated by the {@link JoorCompiler} to compile.
     */
    private String evalCode(JoorCompiler compiler, CamelContext camelContext, String fqn, String script, boolean singleQuotes) {
        // Use reflection as temporary workaround since it is not yet possible
        // Will be fixed by https://issues.apache.org/jira/browse/CAMEL-18977
        try {
            Method m = JoorCompiler.class.getDeclaredMethod("evalCode", CamelContext.class, String.class, String.class,
                    boolean.class);
            m.setAccessible(true);
            return (String) m.invoke(compiler, camelContext, fqn, script, singleQuotes);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Cannot extract the generated code from the compiler", e);
        }
    }
}
