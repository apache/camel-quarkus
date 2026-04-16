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
package org.apache.camel.quarkus.component.support.langchain4j.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * Temporary workaround for a binary incompatibility between quarkus-langchain4j-ollama
 * (compiled against Mutiny 3.1.x with Context.put(String, Object)) and Mutiny 3.2.x
 * (which changed the signature to Context.put(Object, Object)).
 *
 * Remove this class once quarkus-langchain4j is compiled against Mutiny 3.2.x+
 * (i.e. once quarkiverse-langchain4j.version is updated to a version built with Quarkus 3.36+).
 */
@TargetClass(className = "io.quarkiverse.langchain4j.ollama.OllamaStreamingChatLanguageModel", onlyWith = OllamaStreamingSubstitutions.IsPresent.class)
final class OllamaStreamingSubstitutions {

    @Substitute
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        throw new UnsupportedOperationException(
                "OllamaStreamingChatLanguageModel is not supported in native mode due to Mutiny API incompatibility");
    }

    static final class IsPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Thread.currentThread().getContextClassLoader()
                        .loadClass("io.quarkiverse.langchain4j.ollama.OllamaStreamingChatLanguageModel");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
