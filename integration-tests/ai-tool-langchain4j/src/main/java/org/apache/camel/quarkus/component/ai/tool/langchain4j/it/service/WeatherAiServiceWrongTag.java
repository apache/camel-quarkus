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
package org.apache.camel.quarkus.component.ai.tool.langchain4j.it.service;

import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.quarkus.component.ai.tool.langchain4j.it.model.WrongTagToolCallingChatModel;
import org.apache.camel.quarkus.component.support.langchain4j.CamelAiTools;

@ApplicationScoped
@RegisterAiService(chatLanguageModelSupplier = WrongTagToolCallingChatModel.class)
@CamelAiTools("adminTag")
public interface WeatherAiServiceWrongTag {

    String chat(@UserMessage String message);
}
