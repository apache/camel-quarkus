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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import static org.apache.camel.dsl.js.JavaScriptRoutesBuilderLoader.LANGUAGE_ID;

/**
 * {@code JavaScriptDslProcessor} is meant to be used as type of {@link Processor} from a JavaScript file to remain
 * compatible
 * with the native mode that doesn't support the function {@code Java.extend}.
 */
public final class JavaScriptDslProcessor implements Processor {

    /**
     * The name of the argument.
     */
    private final String argumentName;
    /**
     * The source of the processor.
     */
    private final CharSequence source;

    /**
     * Construct a {@code JavaScriptDslProcessor} with the given source and {@code exchange} as argument name.
     *
     * @param source the source of the processor.
     */
    public JavaScriptDslProcessor(CharSequence source) {
        this("exchange", source);
    }

    /**
     * Construct a {@code JavaScriptDslProcessor} with the given source and argument name.
     *
     * @param source the source of the processor.
     */
    public JavaScriptDslProcessor(String argumentName, CharSequence source) {
        this.argumentName = argumentName;
        this.source = source;
    }

    @Override
    public void process(Exchange exchange) {
        try (final Context context = JavaScriptDslHelper.createBuilder().build()) {
            final Value bindings = context.getBindings(LANGUAGE_ID);
            bindings.putMember(argumentName, exchange);
            context.eval(LANGUAGE_ID, source);
        }
    }
}
