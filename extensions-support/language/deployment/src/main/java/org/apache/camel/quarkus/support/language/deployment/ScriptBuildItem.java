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
package org.apache.camel.quarkus.support.language.deployment;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * {@code ScriptBuildItem} represents a script and its binding context in a given language that has been extracted from
 * the route definitions.
 */
public final class ScriptBuildItem extends MultiBuildItem {

    final String language;
    final String content;
    final String loadedContent;
    final Map<String, Object> bindings;

    public ScriptBuildItem(String language, String content, String loadedContent, Map<String, Object> bindings) {
        this.language = language;
        this.content = content;
        this.loadedContent = loadedContent;
        this.bindings = bindings;
    }

    public String getLanguage() {
        return language;
    }

    public String getContent() {
        return content;
    }

    public String getLoadedContent() {
        return loadedContent;
    }

    public Map<String, Object> getBindings() {
        return bindings;
    }
}
