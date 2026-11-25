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
package org.apache.camel.quarkus.component.ai.tool;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.tool.ToolProviderRequest;
import org.apache.camel.component.langchain4j.agent.api.AiAgentWithMemoryService;
import org.apache.camel.component.langchain4j.agent.api.AiAgentWithoutMemoryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamelAiToolProviderTest {

    @Test
    void camelAgentInterfacesAreDetected() {
        assertTrue(CamelAiToolProvider.isCamelAgentRequest(request(AiAgentWithMemoryService.class.getName())));
        assertTrue(CamelAiToolProvider.isCamelAgentRequest(request(AiAgentWithoutMemoryService.class.getName())));
    }

    @Test
    void otherInterfaceIsNotCamelAgent() {
        assertFalse(CamelAiToolProvider.isCamelAgentRequest(request("org.acme.MyAiService")));
    }

    @Test
    void nullInvocationContextIsNotCamelAgent() {
        ToolProviderRequest request = new ToolProviderRequest(builder("org.acme.MyAiService")) {
            @Override
            public InvocationContext invocationContext() {
                return null;
            }
        };
        assertFalse(CamelAiToolProvider.isCamelAgentRequest(request));
    }

    // The constants are names, not class literals: fail when Camel renames or moves an agent service interface
    @Test
    void interfaceNameConstantsResolve() throws ClassNotFoundException {
        assertEquals(2, CamelAiToolProvider.CAMEL_AGENT_SERVICE_INTERFACES.size());
        for (String name : CamelAiToolProvider.CAMEL_AGENT_SERVICE_INTERFACES) {
            Class<?> type = Class.forName(name);
            assertTrue(type == AiAgentWithMemoryService.class || type == AiAgentWithoutMemoryService.class, name);
        }
    }

    private static ToolProviderRequest request(String interfaceName) {
        return new ToolProviderRequest(builder(interfaceName));
    }

    private static ToolProviderRequest.Builder builder(String interfaceName) {
        return ToolProviderRequest.builder()
                .invocationContext(InvocationContext.builder().interfaceName(interfaceName).build())
                .userMessage(UserMessage.from("hello"));
    }
}
