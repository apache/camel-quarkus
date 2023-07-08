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
package org.apache.camel.quarkus.component.joor.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.language.joor.JoorLanguage;
import org.apache.camel.language.joor.JoorMethod;
import org.apache.camel.language.joor.JoorScriptingMethod;

@Recorder
public class JoorExpressionRecorder {

    public RuntimeValue<JoorLanguage> languageNewInstance(JoorExpressionConfig config,
            RuntimeValue<JoorExpressionCompiler.Builder> compilerBuilder,
            RuntimeValue<JoorExpressionScriptingCompiler.Builder> scriptingCompilerBuilder) {
        RuntimeValue<JoorLanguage> language = new RuntimeValue<>(
                new JoorLanguage(compilerBuilder.getValue().build(), scriptingCompilerBuilder.getValue().build()));
        language.getValue().setSingleQuotes(config.singleQuotes);
        config.configResource.ifPresent(language.getValue()::setConfigResource);
        return language;
    }

    public void setResultType(RuntimeValue<JoorLanguage> language, String className) {
        try {
            language.getValue().setResultType(Class.forName(className, true, Thread.currentThread().getContextClassLoader()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RuntimeValue<JoorExpressionCompiler.Builder> expressionCompilerBuilder() {
        return new RuntimeValue<>(new JoorExpressionCompiler.Builder());
    }

    public RuntimeValue<JoorExpressionScriptingCompiler.Builder> expressionScriptingCompilerBuilder() {
        return new RuntimeValue<>(new JoorExpressionScriptingCompiler.Builder());
    }

    public void addExpression(RuntimeValue<JoorExpressionCompiler.Builder> builder, RuntimeValue<CamelContext> ctx, String id,
            String className) {
        try {
            builder.getValue().addExpression(id,
                    (JoorMethod) Class.forName(className, true, Thread.currentThread().getContextClassLoader())
                            .getConstructor(CamelContext.class).newInstance(ctx.getValue()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addScript(RuntimeValue<JoorExpressionScriptingCompiler.Builder> builder, RuntimeValue<CamelContext> ctx,
            String id, String className) {
        try {
            builder.getValue().addScript(id,
                    (JoorScriptingMethod) Class.forName(className, true, Thread.currentThread().getContextClassLoader())
                            .getConstructor(CamelContext.class).newInstance(ctx.getValue()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
