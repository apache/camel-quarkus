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
package org.apache.camel.quarkus.core.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A {@link MultiBuildItem} bearing info about a compiled CSimple language expression.
 */
public final class CompiledCSimpleExpressionBuildItem extends MultiBuildItem {

    private final String sourceCode;
    private final String className;
    private final boolean predicate;

    public CompiledCSimpleExpressionBuildItem(String sourceCode, boolean predicate, String className) {
        this.sourceCode = sourceCode;
        this.predicate = predicate;
        this.className = className;
    }

    /**
     * @return the source code out which the class returned by {@link #getClassName()} was compiled
     */
    public String getSourceCode() {
        return sourceCode;
    }

    /**
     * @return a fully qualified class name compiled from the source code returned by {@link #getSourceCode()}
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return {@code true} if the expression is a predicate; {@code false} otherwise
     */
    public boolean isPredicate() {
        return predicate;
    }

}
