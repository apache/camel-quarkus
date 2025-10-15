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
package org.apache.camel.quarkus.component.langchain4j.agent.it;

import org.apache.camel.builder.RouteBuilder;

public class Langchain4jAgentRoutes extends RouteBuilder {
    public static final String USER_JOHN = "John Doe";

    @Override
    public void configure() throws Exception {
        from("direct:simple-agent")
                .to("langchain4j-agent:test-agent?agent=#simpleAgent");

        from("direct:agent-with-memory")
                .to("langchain4j-agent:test-memory-agent?agent=#agentWithMemory");

        from("direct:agent-with-success-input-guardrail")
                .to("langchain4j-agent:test-agent-with-success-input-guardrail?agent=#agentWithSuccessInputGuardrail");

        from("direct:agent-with-failing-input-guardrail")
                .to("langchain4j-agent:test-agent-with-failing-input-guardrail?agent=#agentWithFailingInputGuardrail");

        from("direct:agent-with-success-output-guardrail")
                .to("langchain4j-agent:test-agent-with-success-output-guardrail?agent=#agentWithSuccessOutputGuardrail");

        from("direct:agent-with-failing-output-guardrail")
                .to("langchain4j-agent:test-agent-with-failing-output-guardrail?agent=#agentWithFailingOutputGuardrail");

        from("direct:agent-with-json-extractor-output-guardrail")
                .to("langchain4j-agent:test-agent-with-json-extractor-output-guardrail?agent=#agentWithJsonExtractorOutputGuardrail");

        from("direct:agent-with-rag")
                .to("langchain4j-agent:test-agent-with-rag?agent=#agentWithRag");

        from("direct:agent-with-custom-service")
                .to("langchain4j-agent:test-agent-with-custom-service?agent=#agentWithCustomService");

        from("direct:agent-with-tools")
                .to("langchain4j-agent:test-agent-with-tools?agent=#agentWithTools&tags=users");

        from("langchain4j-tools:userDb?tags=users&description=Query user database by user ID&parameter.userId=integer")
                .setBody().constant("{\"name\": \"" + USER_JOHN + "\", \"id\": \"123\"}");

        from("direct:agent-with-custom-tools")
                .to("langchain4j-agent:test-agent-custom-tools?agent=#agentWithCustomTools");
    }
}
