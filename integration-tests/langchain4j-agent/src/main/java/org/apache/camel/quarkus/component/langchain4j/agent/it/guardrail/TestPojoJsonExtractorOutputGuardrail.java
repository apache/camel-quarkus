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

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.JsonExtractorOutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import org.apache.camel.quarkus.component.langchain4j.agent.it.model.TestPojo;

public class TestPojoJsonExtractorOutputGuardrail extends JsonExtractorOutputGuardrail<TestPojo> {
    public TestPojoJsonExtractorOutputGuardrail() {
        super(TestPojo.class);
    }

    @Override
    public OutputGuardrailResult validate(AiMessage aiMessage) {
        OutputGuardrailResult parentResult = super.validate(aiMessage);

        if (parentResult.isSuccess()) {
            // Return JSON String representation of TestPojo since that's all the agent can handle
            return OutputGuardrailResult.successWith(trimNonJson(aiMessage.text()));
        }

        // Return failures
        return OutputGuardrailResult.failure(parentResult.failures());
    }
}
