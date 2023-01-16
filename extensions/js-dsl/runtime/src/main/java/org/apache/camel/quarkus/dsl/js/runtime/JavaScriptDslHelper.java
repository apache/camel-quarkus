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
package org.apache.camel.quarkus.dsl.js.runtime;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;

import static org.apache.camel.dsl.js.JavaScriptRoutesBuilderLoader.LANGUAGE_ID;

/**
 * {@code JavaScriptDslHelper} is a utility class for the runtime implementation of the main functional interfaces.
 */
final class JavaScriptDslHelper {

    private JavaScriptDslHelper() {

    }

    /**
     * @return a builder of context properly configured to evaluate a JavaScript expression.
     */
    static Context.Builder createBuilder() {
        return Context.newBuilder(LANGUAGE_ID)
                .allowHostAccess(HostAccess.ALL)
                .allowExperimentalOptions(true)
                .allowHostClassLookup(s -> true)
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .allowIO(true)
                .option("engine.WarnInterpreterOnly", "false");
    }
}
