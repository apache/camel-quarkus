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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.language.joor.JoorCompiler;
import org.apache.camel.language.joor.JoorMethod;
import org.apache.camel.quarkus.support.language.runtime.ExpressionUID;

/**
 * {@code JoorExpressionCompiler} is a jOOR compiler with all the {@link JoorMethod}s defined ahead of time and ready to
 * use at runtime.
 * <p>
 * In case a requested expression is unknown, the compilation is delegated to the parent class which implies that it
 * will only work in JVM mode.
 */
public class JoorExpressionCompiler extends JoorCompiler {

    private final Map<String, JoorMethod> methods;

    private JoorExpressionCompiler(Map<String, JoorMethod> methods) {
        this.methods = Collections.unmodifiableMap(methods);
    }

    @Override
    public JoorMethod compile(CamelContext camelContext, String script, boolean singleQuotes) {
        final JoorMethod method = methods.get(new ExpressionUID(script, singleQuotes).toString());
        if (method == null) {
            return super.compile(camelContext, script, singleQuotes);
        }
        return method;
    }

    public static class Builder {

        private final Map<String, JoorMethod> methods = new HashMap<>();

        public void addExpression(String id, JoorMethod method) {
            methods.put(id, method);
        }

        public JoorExpressionCompiler build() {
            return new JoorExpressionCompiler(methods);
        }
    }
}
