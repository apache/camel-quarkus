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

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class DisabledOnRhelCondition implements ExecutionCondition {
    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled(
            "@DisabledOnRhel: enabled - annotation not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<DisabledOnRhel> annotation = findAnnotation(context.getElement(), DisabledOnRhel.class);
        return annotation.isPresent() ? evaluate(annotation.get().since()) : ENABLED_BY_DEFAULT;
    }

    private ConditionEvaluationResult evaluate(int since) {
        return evaluate(System.getProperty("os.name").toLowerCase(), System.getProperty("os.version").toLowerCase(), since);
    }

    ConditionEvaluationResult evaluate(String osName, String osVersion, int since) {
        Pattern r = Pattern.compile(".+el(\\d+).+");
        Matcher m = r.matcher(osVersion.toLowerCase());
        if (osName.toLowerCase().contains("linux") && m.matches()) {
            if (since == 0) {
                return disabled("@DisabledOnRhel: disable - RHEL");
            } else {
                Integer version = Integer.parseInt(m.group(1));
                if (version.compareTo(since) <= 0) {
                    return disabled("@DisabledOnRhel(since " + since + ") : disable - RHEL");
                }
            }
        }

        return enabled("@DisabledOnRhel: enabled - not a RHEL");
    }
}
