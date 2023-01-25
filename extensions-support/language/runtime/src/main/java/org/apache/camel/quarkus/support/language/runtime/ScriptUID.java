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
package org.apache.camel.quarkus.support.language.runtime;

import java.util.Arrays;
import java.util.Map;

/**
 * {@code ScriptUID} represents the unique identifier of a script, its binding context and its arguments.
 */
public final class ScriptUID extends SourceCodeUID {

    private final String content;
    private final String[] bindings;
    private final Object[] arguments;

    public ScriptUID(String content, Map<String, Object> bindings, Object... arguments) {
        super("Script");
        this.content = content;
        this.bindings = bindings == null || bindings.isEmpty() ? null : bindings.keySet().toArray(new String[0]);
        this.arguments = arguments;
    }

    @Override
    protected String getSourceCode() {
        final StringBuilder source = new StringBuilder();
        source.append(content);
        source.append('.');
        if (bindings != null) {
            source.append(Arrays.toString(bindings));
        }
        source.append('.');
        if (arguments != null) {
            source.append(Arrays.toString(arguments));
        }
        source.append('.');
        return source.toString();
    }
}
