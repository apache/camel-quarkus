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
package org.apache.camel.quarkus.component.langchain4j.chat.ql4j.it;

import org.apache.camel.builder.RouteBuilder;

public class LangChain4jChatQl4jRoutes extends RouteBuilder {
    @Override
    public void configure() {
        // The Quarkus LangChain4j default ChatModel bean is autowired from the registry
        from("direct:defaultModelSimpleMessage")
                .to("langchain4j-chat:defaultSimple?chatOperation=CHAT_SINGLE_MESSAGE");

        from("direct:defaultModelPromptMessage")
                .to("langchain4j-chat:defaultPrompt?chatOperation=CHAT_SINGLE_MESSAGE_WITH_PROMPT");

        from("direct:defaultModelMultipleMessages")
                .to("langchain4j-chat:defaultMultiple?chatOperation=CHAT_MULTIPLE_MESSAGES");

        // The model configured as quarkus.langchain4j.custom.chat-model.provider, selected by name
        from("direct:namedModelSimpleMessage")
                .to("langchain4j-chat:namedSimple?chatOperation=CHAT_SINGLE_MESSAGE&chatModel=#custom");
    }
}
