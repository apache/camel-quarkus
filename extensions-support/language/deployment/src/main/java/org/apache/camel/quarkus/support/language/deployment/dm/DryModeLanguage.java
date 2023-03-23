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
package org.apache.camel.quarkus.support.language.deployment.dm;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.support.LanguageSupport;

/**
 * {@code DryModeLanguage} is a mock language meant to collect all the expressions and predicates that are registered
 * for a specific language.
 */
public class DryModeLanguage extends LanguageSupport {

    private final String name;
    private final Predicate defaultPredicate = new DryModePredicate();
    private final Expression defaultExpression = new DryModeExpression();
    private final Map<Boolean, Set<ExpressionHolder>> expressions = new ConcurrentHashMap<>();

    DryModeLanguage(CamelContext camelContext, String name) {
        this.name = name;
        this.setCamelContext(camelContext);
    }

    public String getName() {
        return name;
    }

    public Set<ExpressionHolder> getPredicates() {
        return expressions.getOrDefault(Boolean.TRUE, Set.of());
    }

    public Set<ExpressionHolder> getExpressions() {
        return expressions.getOrDefault(Boolean.FALSE, Set.of());
    }

    public Set<ScriptHolder> getScripts() {
        return Set.of();
    }

    @Override
    public Predicate createPredicate(String expression) {
        expressions.computeIfAbsent(Boolean.TRUE, mode -> ConcurrentHashMap.newKeySet())
                .add(new ExpressionHolder(expression, loadResource(expression)));
        return defaultPredicate;
    }

    @Override
    public Expression createExpression(String expression) {
        expressions.computeIfAbsent(Boolean.FALSE, mode -> ConcurrentHashMap.newKeySet())
                .add(new ExpressionHolder(expression, loadResource(expression)));
        return defaultExpression;
    }

    @Override
    public Predicate createPredicate(String expression, Object[] properties) {
        expressions.computeIfAbsent(Boolean.TRUE, mode -> ConcurrentHashMap.newKeySet())
                .add(new ExpressionHolder(expression, loadResource(expression), properties));
        return defaultPredicate;
    }

    @Override
    public Expression createExpression(String expression, Object[] properties) {
        expressions.computeIfAbsent(Boolean.FALSE, mode -> ConcurrentHashMap.newKeySet())
                .add(new ExpressionHolder(expression, loadResource(expression), properties));
        return defaultExpression;
    }

    private static class DryModePredicate implements Predicate {

        @Override
        public boolean matches(Exchange exchange) {
            return false;
        }

        @Override
        public void init(CamelContext context) {
            // nothing to do
        }

        @Override
        public void initPredicate(CamelContext context) {
            // nothing to do
        }
    }

    private static class DryModeExpression implements Expression {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T evaluate(Exchange exchange, Class<T> type) {
            // A non-null value must be returned and the returned type is not really important for the dry mode
            return (T) new Object();
        }

        @Override
        public void init(CamelContext context) {
            // nothing to do
        }
    }
}
