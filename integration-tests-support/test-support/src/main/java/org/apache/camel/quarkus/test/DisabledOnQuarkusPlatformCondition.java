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

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class DisabledOnQuarkusPlatformCondition implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled(
            "@DisabledOnQuarkusPlatform: enabled - annotation not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return findAnnotation(context.getElement(), DisabledOnQuarkusPlatform.class).isPresent() ? evaluate()
                : ENABLED_BY_DEFAULT;
    }

    private ConditionEvaluationResult evaluate() {
        // There are no .java source files in Quarkus Platform (tests are run from test-jars)
        if (Files.exists(Paths.get("src/main/java"))) {
            return enabled("@DisabledOnQuarkusPlatform: enabled - Quarkus Platform not detected");
        } else {
            return disabled("@DisabledOnQuarkusPlatform: disabled - Quarkus Platform detected");
        }
    }
}
