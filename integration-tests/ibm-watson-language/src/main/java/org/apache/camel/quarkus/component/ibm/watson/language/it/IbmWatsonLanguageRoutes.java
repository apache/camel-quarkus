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
package org.apache.camel.quarkus.component.ibm.watson.language.it;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.ibm.watson.language.WatsonLanguageConstants;
import org.apache.camel.component.ibm.watson.language.WatsonLanguageOperations;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class IbmWatsonLanguageRoutes extends RouteBuilder {

    @ConfigProperty(name = "camel.ibm.watson.serviceUrl")
    Optional<String> serviceUrl;

    @ConfigProperty(name = "camel.ibm.watson.apiKey")
    Optional<String> apiKey;

    @Override
    public void configure() {
        if (serviceUrl.isPresent() && apiKey.isPresent()) {
            String baseUri = String.format("ibm-watson-language:test?serviceUrl=RAW(%s)&apiKey=RAW(%s)",
                    serviceUrl.get(), apiKey.get());

            // Analyze text with sentiment
            from("direct:analyze-text-sentiment")
                    .setHeader(WatsonLanguageConstants.OPERATION, constant(WatsonLanguageOperations.analyzeText))
                    .setHeader(WatsonLanguageConstants.ANALYZE_SENTIMENT, constant(true))
                    .to(baseUri);

            // Analyze text with emotion
            from("direct:analyze-text-emotion")
                    .setHeader(WatsonLanguageConstants.OPERATION, constant(WatsonLanguageOperations.analyzeText))
                    .setHeader(WatsonLanguageConstants.ANALYZE_EMOTION, constant(true))
                    .to(baseUri);

            // Analyze text with entities
            from("direct:analyze-text-entities")
                    .setHeader(WatsonLanguageConstants.OPERATION, constant(WatsonLanguageOperations.analyzeText))
                    .setHeader(WatsonLanguageConstants.ANALYZE_ENTITIES, constant(true))
                    .to(baseUri);

            // Analyze text with keywords
            from("direct:analyze-text-keywords")
                    .setHeader(WatsonLanguageConstants.OPERATION, constant(WatsonLanguageOperations.analyzeText))
                    .setHeader(WatsonLanguageConstants.ANALYZE_KEYWORDS, constant(true))
                    .to(baseUri);

            // Analyze text with concepts
            from("direct:analyze-text-concepts")
                    .setHeader(WatsonLanguageConstants.OPERATION, constant(WatsonLanguageOperations.analyzeText))
                    .setHeader(WatsonLanguageConstants.ANALYZE_CONCEPTS, constant(true))
                    .to(baseUri);

            // Analyze text with categories
            from("direct:analyze-text-categories")
                    .setHeader(WatsonLanguageConstants.OPERATION, constant(WatsonLanguageOperations.analyzeText))
                    .setHeader(WatsonLanguageConstants.ANALYZE_CATEGORIES, constant(true))
                    .to(baseUri);

            // Analyze URL operation
            from("direct:analyze-url")
                    .setHeader(WatsonLanguageConstants.OPERATION, constant(WatsonLanguageOperations.analyzeUrl))
                    .to(baseUri);
        }
    }
}
