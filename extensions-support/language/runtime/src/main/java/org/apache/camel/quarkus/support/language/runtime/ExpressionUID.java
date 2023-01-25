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
package org.apache.camel.quarkus.support.language.runtime;

import java.util.Arrays;
import java.util.Objects;

/**
 * {@code ExpressionUID} represents the unique identifier of a simple expression and its arguments.
 */
public final class ExpressionUID extends SourceCodeUID {

    private final String expression;
    private final String[] arguments;

    public ExpressionUID(String expression) {
        super("Expression");
        this.expression = expression;
        this.arguments = null;
    }

    public ExpressionUID(String expression, Object... arguments) {
        super("Expression");
        this.expression = expression;
        this.arguments = arguments == null || arguments.length == 0 ? null
                : Arrays.stream(arguments).map(Objects::toString).toArray(String[]::new);
    }

    @Override
    protected String getSourceCode() {
        final StringBuilder source = new StringBuilder();
        source.append(expression);
        source.append('.');
        if (arguments != null) {
            source.append(Arrays.toString(arguments));
        }
        source.append('.');
        return source.toString();
    }
}
