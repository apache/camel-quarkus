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
import org.apache.camel.language.joor.JoorScriptingCompiler;
import org.apache.camel.language.joor.JoorScriptingMethod;
import org.apache.camel.quarkus.support.language.runtime.ScriptUID;

/**
 * {@code JoorExpressionScriptingCompiler} is a jOOR scripting compiler with all the {@link JoorScriptingMethod}s
 * defined ahead of time and ready to use at runtime.
 * <p>
 * In case a requested script is unknown, the compilation is delegated to the parent class which implies that it will
 * only work in JVM mode.
 */
public class JoorExpressionScriptingCompiler extends JoorScriptingCompiler {

    private final Map<String, JoorScriptingMethod> methods;

    private JoorExpressionScriptingCompiler(Map<String, JoorScriptingMethod> methods) {
        this.methods = Collections.unmodifiableMap(methods);
    }

    @Override
    public JoorScriptingMethod compile(CamelContext camelContext, String script, Map<String, Object> bindings,
            boolean singleQuotes) {
        final JoorScriptingMethod method = methods.get(new ScriptUID(script, bindings, singleQuotes).toString());
        if (method == null) {
            return super.compile(camelContext, script, bindings, singleQuotes);
        }
        return method;
    }

    public static class Builder {
        private final Map<String, JoorScriptingMethod> methods = new HashMap<>();

        public void addScript(String id, JoorScriptingMethod method) {
            methods.put(id, method);
        }

        public JoorExpressionScriptingCompiler build() {
            return new JoorExpressionScriptingCompiler(methods);
        }
    }
}
