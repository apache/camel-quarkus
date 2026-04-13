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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@EnabledIfSystemProperty(named = "camel.ibm.watson.apiKey", matches = ".*", disabledReason = "IBM Watson API Key not provided")
@EnabledIfSystemProperty(named = "camel.ibm.watson.serviceUrl", matches = ".*", disabledReason = "IBM Watson Service URL not provided")
@QuarkusTest
class IbmWatsonLanguageTest {

    @Test
    public void testAnalyzeTextSentiment() {
        String text = "I love using Apache Camel for integration! It makes my life so much easier.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(text)
                .post("/ibm-watson-language/analyze/text/sentiment")
                .then()
                .statusCode(200)
                .body("sentiment", notNullValue())
                .body("sentiment.document", notNullValue())
                .body("sentiment.document.label", is("positive"))
                .body("language", equalTo("en"));
    }

    @Test
    public void testAnalyzeTextEmotion() {
        String text = "I am excited about this new feature! It's going to be amazing.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(text)
                .post("/ibm-watson-language/analyze/text/emotion")
                .then()
                .statusCode(200)
                .body("emotion", notNullValue())
                .body("emotion.document", notNullValue())
                .body("emotion.document.emotion", hasKey("joy"))
                .body("emotion.document.emotion", hasKey("sadness"))
                .body("language", equalTo("en"));
    }

    @Test
    public void testAnalyzeTextEntities() {
        String text = "IBM Watson is an artificial intelligence technology developed in New York.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(text)
                .post("/ibm-watson-language/analyze/text/entities")
                .then()
                .statusCode(200)
                .body("entities", notNullValue())
                .body("entities.size()", not(0))
                .body("language", equalTo("en"));
    }

    @Test
    public void testAnalyzeTextKeywords() {
        String text = "Apache Camel Quarkus provides fast startup times and low memory footprint for microservices.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(text)
                .post("/ibm-watson-language/analyze/text/keywords")
                .then()
                .statusCode(200)
                .body("keywords", notNullValue())
                .body("keywords.size()", not(0))
                .body("language", equalTo("en"));
    }

    @Test
    public void testAnalyzeTextConcepts() {
        String text = "Machine learning and artificial intelligence are transforming the technology industry.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(text)
                .post("/ibm-watson-language/analyze/text/concepts")
                .then()
                .statusCode(200)
                .body("concepts", notNullValue())
                .body("concepts.size()", not(0))
                .body("language", equalTo("en"));
    }

    @Test
    public void testAnalyzeTextCategories() {
        String text = "The stock market experienced significant volatility today as investors reacted to the latest economic data.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(text)
                .post("/ibm-watson-language/analyze/text/categories")
                .then()
                .statusCode(200)
                .body("categories", notNullValue())
                .body("categories.size()", not(0))
                .body("language", equalTo("en"));
    }

    @Test
    public void testAnalyzeUrl() {
        String url = "https://www.ibm.com/watson";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(url)
                .post("/ibm-watson-language/analyze/url")
                .then()
                .statusCode(200)
                .body("language", notNullValue())
                .body("retrievedUrl", equalTo(url));
    }
}
