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
package org.apache.camel.quarkus.support.language.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * {@code ExpressionBuildItem} represents an expression in a given language that has been extracted from the route
 * definitions.
 */
public final class ExpressionBuildItem extends MultiBuildItem {

    final String language;
    final String expression;
    final boolean predicate;
    final Object[] properties;

    public ExpressionBuildItem(String language, String expression, Object[] properties, boolean predicate) {
        this.language = language;
        this.expression = expression;
        this.properties = properties;
        this.predicate = predicate;
    }

    public String getLanguage() {
        return language;
    }

    public String getExpression() {
        return expression;
    }

    public boolean isPredicate() {
        return predicate;
    }

    public Object[] getProperties() {
        return properties;
    }
}
