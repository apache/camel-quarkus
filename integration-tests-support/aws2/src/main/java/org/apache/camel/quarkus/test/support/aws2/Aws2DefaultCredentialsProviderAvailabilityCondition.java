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
package org.apache.camel.quarkus.test.support.aws2;

import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;

public class Aws2DefaultCredentialsProviderAvailabilityCondition implements ExecutionCondition {

    public static final String UNABLE_TO_LOAD_CREDENTIALS_MSG = "Unable to load credentials";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (!MockBackendUtils.startMockBackend(false)) {
            return ConditionEvaluationResult.disabled("Test can be executed only with mock backend.");
        }
        try {
            DefaultCredentialsProvider.create().resolveCredentials();
        } catch (Exception e) {
            //if message starts with "Unable to load credentials", allow testing
            if (e instanceof SdkClientException && e.getMessage() != null
                    && e.getMessage().startsWith(UNABLE_TO_LOAD_CREDENTIALS_MSG)) {
                return ConditionEvaluationResult
                        .enabled("DefaultCredentialsProvider is NOT defined in the system, Testing can proceed.");
            }
        }
        return ConditionEvaluationResult.disabled("DefaultCredentialsProvider is already defined in the system.");
    }

}
