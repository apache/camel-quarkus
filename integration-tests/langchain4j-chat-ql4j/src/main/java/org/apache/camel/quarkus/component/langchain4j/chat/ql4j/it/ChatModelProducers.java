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

import dev.langchain4j.model.chat.ChatModel;
import io.quarkiverse.langchain4j.ModelName;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChatModelProducers {
    // Quarkus LangChain4j only creates model beans for models that have injection points.
    // This injection point forces the creation of the default ChatModel bean so that the
    // Camel langchain4j-chat component can autowire it from the registry
    @Inject
    ChatModel defaultChatModel;

    // Quarkus LangChain4j named model beans only have the @ModelName qualifier, which the Camel
    // registry cannot look up by name. Re-expose the named model under an @Identifier so that
    // routes can reference it via chatModel=#customChatModel
    @Produces
    @Identifier("customChatModel")
    ChatModel customChatModel(@ModelName("custom") ChatModel chatModel) {
        return chatModel;
    }
}
