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

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class LangChain4jChatQl4jRoutes extends RouteBuilder {
    @Override
    public void configure() {
        // The default Quarkus LangChain4j ChatModel bean is autowired from the registry
        from("direct:defaultModelSimpleMessage")
                .to("langchain4j-chat:defaultModelSimple?chatOperation=CHAT_SINGLE_MESSAGE");

        from("direct:defaultModelPromptMessage")
                .to("langchain4j-chat:defaultModelPrompt?chatOperation=CHAT_SINGLE_MESSAGE_WITH_PROMPT");

        from("direct:defaultModelMultipleMessages")
                .to("langchain4j-chat:defaultModelMultiple?chatOperation=CHAT_MULTIPLE_MESSAGES");

        // The Quarkus LangChain4j @ModelName("custom") ChatModel bean re-exposed as customChatModel
        from("direct:namedModelSimpleMessage")
                .to("langchain4j-chat:namedModelSimple?chatOperation=CHAT_SINGLE_MESSAGE&chatModel=#customChatModel");
    }
}
