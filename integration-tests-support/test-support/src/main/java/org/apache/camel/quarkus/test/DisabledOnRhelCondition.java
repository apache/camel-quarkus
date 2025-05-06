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

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class DisabledOnRhelCondition implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled(
            "@DisabledOnRhel: enabled - annotation not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        boolean it = AnnotationUtils.findAnnotation(context.getTestClass(), QuarkusIntegrationTest.class).isPresent()
                || AnnotationUtils.findAnnotation(context.getTestClass(), QuarkusMainIntegrationTest.class).isPresent();
        Optional<DisabledOnRhel> annotation = findAnnotation(context.getElement(), DisabledOnRhel.class);
        return annotation.isPresent() ? evaluate(annotation.get().version(), annotation.get().integrationTests(), it)
                : ENABLED_BY_DEFAULT;
    }

    private ConditionEvaluationResult evaluate(int version, boolean shouldDisableOnIt, boolean it) {
        return evaluate(System.getProperty("os.name").toLowerCase(), System.getProperty("os.version").toLowerCase(), version,
                shouldDisableOnIt, it);
    }

    ConditionEvaluationResult evaluate(String osName, String osVersion, int version, boolean shouldDisableOnIt, boolean it) {
        Pattern r = Pattern.compile(".+el(\\d+).+");
        Matcher m = r.matcher(osVersion.toLowerCase());
        if (osName.toLowerCase().contains("linux") && m.matches()) {
            if (version == 0) {
                return disabled("@DisabledOnRhel: disable - RHEL");
            } else {
                Integer rhelVersion = Integer.parseInt(m.group(1));
                if (version == rhelVersion) {

                    //check second condition - integrationTests
                    if (shouldDisableOnIt && it) {
                        return disabled("@DisabledOnRhel(" + version + ", native = true) : disable - RHEL");
                    }
                    if (!shouldDisableOnIt) {
                        return disabled("@DisabledOnRhel(" + version + ") : disable - RHEL");
                    }
                }
            }
        }

        return enabled("@DisabledOnRhel: enabled - not a RHEL / or native");
    }
}
