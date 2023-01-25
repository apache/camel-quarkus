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

import java.lang.reflect.Field;

import org.apache.camel.language.joor.JoorCompiler;
import org.apache.camel.language.joor.JoorLanguage;
import org.apache.camel.language.joor.JoorScriptingCompiler;

/**
 * {@code JoorExpressionLanguage} is a jOOR language which uses a specific jOOR compiler and jOOR scripting compiler
 * that can be preloaded during the static initialization phase.
 */
public class JoorExpressionLanguage extends JoorLanguage {

    public static final String PACKAGE_NAME = "org.apache.camel.quarkus.component.joor.generated";

    public JoorCompiler getJoorCompiler() {
        // Use reflection as temporary workaround since it is not yet possible
        // Will be fixed by https://issues.apache.org/jira/browse/CAMEL-18977
        try {
            Field f = JoorLanguage.class.getDeclaredField("compiler");
            f.setAccessible(true);
            return (JoorCompiler) f.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Cannot extract the compiler from the language", e);
        }
    }

    public void setJoorCompiler(JoorCompiler compiler) {
        // Use reflection as temporary workaround since it is not yet possible
        // Will be fixed by https://issues.apache.org/jira/browse/CAMEL-18977
        try {
            Field f = JoorLanguage.class.getDeclaredField("compiler");
            f.setAccessible(true);
            f.set(this, compiler);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Cannot set the compiler to the language", e);
        }
    }

    public JoorScriptingCompiler getJoorScriptingCompiler() {
        // Use reflection as temporary workaround since it is not yet possible
        // Will be fixed by https://issues.apache.org/jira/browse/CAMEL-18977
        try {
            Field f = JoorLanguage.class.getDeclaredField("scriptingCompiler");
            f.setAccessible(true);
            return (JoorScriptingCompiler) f.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Cannot extract the scripting compiler from the language", e);
        }
    }

    public void setJoorScriptingCompiler(JoorScriptingCompiler compiler) {
        // Use reflection as temporary workaround since it is not yet possible
        // Will be fixed by https://issues.apache.org/jira/browse/CAMEL-18977
        try {
            Field f = JoorLanguage.class.getDeclaredField("scriptingCompiler");
            f.setAccessible(true);
            f.set(this, compiler);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Cannot set the scripting compiler to the language", e);
        }
    }
}
