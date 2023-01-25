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
import java.util.Objects;

/**
 * {@code ScriptHolder} represents a script with its binding context that could be extracted during the dry run.
 */
public final class ScriptHolder {

    private final String content;

    private final Map<String, Object> bindings;

    public ScriptHolder(String content, Map<String, Object> bindings) {
        this.content = content;
        this.bindings = bindings;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getBindings() {
        return bindings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScriptHolder script = (ScriptHolder) o;
        return Objects.equals(content, script.content) && Objects.equals(bindings, script.bindings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, bindings);
    }
}
