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
package org.apache.camel.quarkus.test;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.Preconditions;

import static java.lang.String.format;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

class EnabledIfPropertyCondition implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@EnabledIfProperty is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return findAnnotation(context.getElement(), EnabledIfProperty.class).map(this::map)
                .orElse(ENABLED_BY_DEFAULT);
    }

    private ConditionEvaluationResult map(EnabledIfProperty annotation) {
        final String name = annotation.named().trim();
        final String regex = annotation.matches();

        Preconditions.notBlank(name, () -> "The 'named' attribute must not be blank in " + annotation);
        Preconditions.notBlank(regex, () -> "The 'matches' attribute must not be blank in " + annotation);

        return ConfigProviderResolver.instance().getConfig().getOptionalValue(name, String.class)
                .map(actual -> {
                    return actual.matches(regex)
                            ? enabled(
                                    format("Config property [%s] with value [%s] matches regular expression [%s]",
                                            name, actual, regex))
                            : disabled(
                                    format("Config property [%s] with value [%s] does not match regular expression [%s]",
                                            name, actual, regex));
                })
                .orElseGet(() -> {
                    return disabled(
                            format("Config property [%s] does not exist", name));
                });
    }
}
