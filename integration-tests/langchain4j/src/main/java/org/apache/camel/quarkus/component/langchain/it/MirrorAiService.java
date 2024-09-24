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
package org.apache.camel.quarkus.component.langchain.it;

import java.util.function.Supplier;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Handler;
import org.apache.camel.Header;
import org.apache.camel.jsonpath.JsonPath;
import org.apache.camel.quarkus.component.langchain.it.MirrorAiService.MirrorModelSupplier;

@ApplicationScoped
@RegisterAiService(chatLanguageModelSupplier = MirrorModelSupplier.class)
public interface MirrorAiService {

    public static class MirrorModelSupplier implements Supplier<ChatLanguageModel> {
        @Override
        public ChatLanguageModel get() {
            return (messages) -> new Response<>(new AiMessage(messages.get(0).text()));
        }
    }

    @UserMessage("{\"fromJsonPath\": \"{fromJsonPath}\", \"fromHeader\": \"{fromHeader}\"}")
    @Handler
    String invoke(@JsonPath("$.included") String fromJsonPath, @Header("headerName") String fromHeader);
}
