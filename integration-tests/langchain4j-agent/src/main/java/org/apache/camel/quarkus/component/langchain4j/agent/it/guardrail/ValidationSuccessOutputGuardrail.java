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
package org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

public class ValidationSuccessOutputGuardrail implements OutputGuardrail {
    private static final AtomicBoolean validateCalled = new AtomicBoolean(false);

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        validateCalled.set(true);
        return OutputGuardrailResult.success();
    }

    public static boolean isValidateCalled() {
        return validateCalled.get();
    }
}
