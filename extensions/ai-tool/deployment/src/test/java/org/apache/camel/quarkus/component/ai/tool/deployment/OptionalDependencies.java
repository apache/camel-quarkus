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
package org.apache.camel.quarkus.component.ai.tool.deployment;

import dev.langchain4j.model.chat.ChatModel;
import io.quarkus.maven.dependency.Dependency;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * The optional dependencies of the Camel AI tool bridge, added per test, and a {@link ChatModel} bean that Quarkus
 * LangChain4j requires once it is present.
 */
@Singleton
public class OptionalDependencies {

    static final Dependency QUARKUS_LANGCHAIN4J = Dependency.of("io.quarkiverse.langchain4j", "quarkus-langchain4j-core",
            System.getProperty("quarkiverse-langchain4j.version"));
    static final Dependency CAMEL_LANGCHAIN4J_AGENT = Dependency.of("org.apache.camel", "camel-langchain4j-agent",
            System.getProperty("camel.version"));

    @Produces
    ChatModel chatModel() {
        return new ChatModel() {
        };
    }
}
