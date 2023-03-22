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
package org.apache.camel.quarkus.component.groovy.runtime;

import groovy.lang.Script;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.language.groovy.GroovyLanguage;

@Recorder
public class GroovyExpressionRecorder {

    public RuntimeValue<GroovyLanguage.Builder> languageBuilder() {
        return new RuntimeValue<>(new GroovyLanguage.Builder());
    }

    @SuppressWarnings("unchecked")
    public void addScript(RuntimeValue<GroovyLanguage.Builder> builder, String content, Class<?> clazz) {
        try {
            builder.getValue().addScript(content, (Class<Script>) clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RuntimeValue<GroovyLanguage> languageNewInstance(RuntimeValue<GroovyLanguage.Builder> builder) {
        return new RuntimeValue<>(builder.getValue().build());
    }
}
