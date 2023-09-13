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
package org.apache.camel.quarkus.component.telegram.it;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CustomWebhookCondition implements ExecutionCondition {
    private static final String TELEGRAM_WEBHOOK_DISABLED = "TELEGRAM_WEBHOOK_DISABLED";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        boolean isWebhookDisabled = System.getenv(TELEGRAM_WEBHOOK_DISABLED) != null
                && Boolean.parseBoolean(System.getenv(TELEGRAM_WEBHOOK_DISABLED));

        if (isWebhookDisabled) {
            return ConditionEvaluationResult.disabled("Webhook test is disabled");
        }

        return ConditionEvaluationResult.enabled("Webhook test is enabled");
    }
}
