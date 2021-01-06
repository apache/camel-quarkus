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
package org.apache.camel.quarkus.component.csimple.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A {@link MultiBuildItem} bearing info about a CSimple language expression that needs to get compiled.
 */
public final class CSimpleExpressionSourceBuildItem extends MultiBuildItem {

    private final String sourceCode;
    private final String classNameBase;
    private final boolean predicate;

    public CSimpleExpressionSourceBuildItem(String sourceCode, boolean predicate, String classNameBase) {
        this.sourceCode = sourceCode;
        this.predicate = predicate;
        this.classNameBase = classNameBase;
    }

    /**
     * @return the expression source code to compile
     */
    public String getSourceCode() {
        return sourceCode;
    }

    /**
     * @return a fully qualified class name that the compiler may use as a base for the name of the class into which it
     *         compiles the source code returned by {@link #getSourceCode()}
     */
    public String getClassNameBase() {
        return classNameBase;
    }

    /**
     * @return {@code true} if the expression is a predicate; {@code false} otherwise
     */
    public boolean isPredicate() {
        return predicate;
    }

}
