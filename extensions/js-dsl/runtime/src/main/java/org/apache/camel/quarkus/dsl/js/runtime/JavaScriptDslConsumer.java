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

import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import static org.apache.camel.dsl.js.JavaScriptRoutesBuilderLoader.LANGUAGE_ID;

/**
 * {@code JavaScriptDslConsumer} is meant to be used as type of {@link Consumer} from a JavaScript file to remain
 * compatible
 * with the native mode that doesn't support the function {@code Java.extend}.
 *
 * @param <T> the type of the input to the operation
 */
public final class JavaScriptDslConsumer<T> implements Consumer<T> {

    /**
     * The name of the argument.
     */
    private final String argumentName;
    /**
     * The source of the consumer.
     */
    private final CharSequence source;

    /**
     * Construct a {@code JavaScriptDslConsumer} with the given source and {@code t} as argument name.
     *
     * @param source the source of the consumer.
     */
    public JavaScriptDslConsumer(CharSequence source) {
        this("t", source);
    }

    /**
     * Construct a {@code JavaScriptDslConsumer} with the given source and argument name.
     *
     * @param argumentName the name of the argument.
     * @param source       the source of the consumer.
     */
    public JavaScriptDslConsumer(String argumentName, CharSequence source) {
        this.argumentName = argumentName;
        this.source = source;
    }

    @Override
    public void accept(T t) {
        try (final Context context = JavaScriptDslHelper.createBuilder().build()) {
            final Value bindings = context.getBindings(LANGUAGE_ID);
            bindings.putMember(argumentName, t);
            context.eval(LANGUAGE_ID, source);
        }
    }
}
