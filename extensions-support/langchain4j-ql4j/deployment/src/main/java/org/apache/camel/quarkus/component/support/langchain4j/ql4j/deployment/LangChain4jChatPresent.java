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
package org.apache.camel.quarkus.component.support.langchain4j.ql4j.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

public class LangChain4jChatPresent implements BooleanSupplier {
    private static final String LANGCHAIN4J_CHAT_COMPONENT_CLASS = "org.apache.camel.component.langchain4j.chat.LangChain4jChatComponent";

    @Override
    public boolean getAsBoolean() {
        return QuarkusClassLoader.isClassPresentAtRuntime(LANGCHAIN4J_CHAT_COMPONENT_CLASS);
    }
}
