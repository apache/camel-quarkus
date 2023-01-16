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

import java.util.function.BiPredicate;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import static org.apache.camel.dsl.js.JavaScriptRoutesBuilderLoader.LANGUAGE_ID;

/**
 * {@code JavaScriptDslBiPredicate} is meant to be used as type of {@link BiPredicate} from a JavaScript file to remain
 * compatible
 * with the native mode that doesn't support the function {@code Java.extend}.
 *
 * @param <T> the type of the first argument to the predicate
 * @param <U> the type of the second argument to the predicate
 */
public final class JavaScriptDslBiPredicate<T, U> implements BiPredicate<T, U> {

    /**
     * The name of the first argument.
     */
    private final String firstArgumentName;
    /**
     * The name of the second argument.
     */
    private final String secondArgumentName;
    /**
     * The source of the predicate.
     */
    private final CharSequence source;

    /**
     * Construct a {@code JavaScriptDslBiPredicate} with the given source, {@code t} as first argument name and {@code u} as
     * second argument.
     *
     * @param source the source of the predicate.
     */
    public JavaScriptDslBiPredicate(CharSequence source) {
        this("t", "u", source);
    }

    /**
     * Construct a {@code JavaScriptDslBiPredicate} with the given source and argument names.
     *
     * @param firstArgumentName  the name of the first argument.
     * @param secondArgumentName the name of the second argument.
     * @param source             the source of the consumer.
     */
    public JavaScriptDslBiPredicate(String firstArgumentName, String secondArgumentName, CharSequence source) {
        this.firstArgumentName = firstArgumentName;
        this.secondArgumentName = secondArgumentName;
        this.source = source;
    }

    @Override
    public boolean test(T t, U u) {
        try (final Context context = JavaScriptDslHelper.createBuilder().build()) {
            final Value bindings = context.getBindings(LANGUAGE_ID);
            bindings.putMember(firstArgumentName, t);
            bindings.putMember(secondArgumentName, u);
            Value value = context.eval(LANGUAGE_ID, source);
            return value != null && value.asBoolean();
        }
    }
}
