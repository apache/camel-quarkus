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
package org.apache.camel.quarkus.component.langchain4j.tokenizer.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.tokenizer.LangChain4jTokenizerDefinition;

public class Langchain4jTokenizerRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:tokenizeLines")
                .tokenize(tokenizer()
                        .byLine()
                        .maxTokens(1024)
                        .maxOverlap(10)
                        .using(LangChain4jTokenizerDefinition.TokenizerType.OPEN_AI)
                        .end())
                .split()
                .body()
                .to("mock:tokenizeLines");

        from("direct:tokenizeParagraphs")
                .tokenize(tokenizer()
                        .byParagraph()
                        .maxTokens(1024)
                        .maxOverlap(10)
                        .using(LangChain4jTokenizerDefinition.TokenizerType.OPEN_AI)
                        .end())
                .split().body()
                .to("mock:tokenizeParagraphs");

        from("direct:tokenizeSentences")
                .tokenize(tokenizer()
                        .bySentence()
                        .maxTokens(1024)
                        .maxOverlap(10)
                        .using(LangChain4jTokenizerDefinition.TokenizerType.OPEN_AI)
                        .end())
                .split().body()
                .to("mock:tokenizeSentences");
    }
}
