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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.NoSuchLanguageException;
import org.apache.camel.impl.engine.DefaultLanguageResolver;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.ScriptingLanguage;

/**
 * {@code DryModeLanguageResolver} is used to resolve all languages with {@link DryModeLanguage} and scripting languages
 * with {@link DryModeScriptingLanguage} for a dry run.
 */
class DryModeLanguageResolver extends DefaultLanguageResolver {

    private final Map<String, DryModeLanguage> languages = new ConcurrentHashMap<>();

    @Override
    public Language resolveLanguage(String name, CamelContext context) throws NoSuchLanguageException {
        final Language language = super.resolveLanguage(name, context);
        if (language instanceof ScriptingLanguage) {
            return languages.computeIfAbsent(name, DryModeScriptingLanguage::new);
        }
        return languages.computeIfAbsent(name, DryModeLanguage::new);
    }

    Collection<DryModeLanguage> getLanguages() {
        return languages.values();
    }
}
