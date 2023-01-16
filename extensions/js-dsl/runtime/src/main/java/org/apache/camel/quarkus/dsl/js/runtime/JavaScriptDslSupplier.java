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

import java.util.function.Supplier;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import static org.apache.camel.dsl.js.JavaScriptRoutesBuilderLoader.LANGUAGE_ID;

/**
 * {@code JavaScriptDslSupplier} is meant to be used as type of {@link Supplier} from a JavaScript file to remain
 * compatible
 * with the native mode that doesn't support the function {@code Java.extend}.
 *
 * @param <T> the type of results supplied by this supplier
 */
public final class JavaScriptDslSupplier<T> implements Supplier<T> {

    /**
     * The source of the supplier.
     */
    private final CharSequence source;

    /**
     * Construct a {@code JavaScriptDslSupplier} with the given source.
     *
     * @param source the source of the supplier.
     */
    public JavaScriptDslSupplier(CharSequence source) {
        this.source = source;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        try (final Context context = JavaScriptDslHelper.createBuilder().build()) {
            Value value = context.eval(LANGUAGE_ID, source);
            return value == null ? null : (T) value.as(Object.class);
        }
    }
}
