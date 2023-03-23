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
package org.apache.camel.quarkus.support.language.deployment.dm;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ScriptingLanguage;

/**
 * {@code DryModeScriptingLanguage} is a mock scripting language meant to collect all the expressions,
 * predicates and scripts that are registered for a specific scripting language.
 */
class DryModeScriptingLanguage extends DryModeLanguage implements ScriptingLanguage {

    private final Set<ScriptHolder> scripts = ConcurrentHashMap.newKeySet();

    DryModeScriptingLanguage(CamelContext camelContext, String name) {
        super(camelContext, name);
    }

    @Override
    public Set<ScriptHolder> getScripts() {
        return scripts;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T evaluate(String script, Map<String, Object> bindings, Class<T> resultType) {
        scripts.add(new ScriptHolder(script, loadResource(script), bindings));
        // A non-null value must be returned and the returned type is not really important for the dry mode
        return (T) new Object();
    }
}
