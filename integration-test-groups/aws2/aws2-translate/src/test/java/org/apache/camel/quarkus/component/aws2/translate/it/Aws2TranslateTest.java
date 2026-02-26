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
package org.apache.camel.quarkus.component.aws2.translate.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.apache.camel.quarkus.test.support.aws2.BaseAWs2TestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2TranslateTest extends BaseAWs2TestSupport {

    public Aws2TranslateTest() {
        super("/aws2-translate");
    }

    @Test
    public void translate() {
        RestAssured.given()
                .queryParam("sourceLanguage", "en")
                .queryParam("targetLanguage", "fr")
                .body("Hello")
                .post("/aws2-translate/translate")
                .then()
                .statusCode(200)
                .body(is("Bonjour"));
    }

    @Override
    public void testMethodForDefaultCredentialsProvider() {
        RestAssured.given()
                .queryParam("sourceLanguage", "en")
                .queryParam("targetLanguage", "fr")
                .body("Hello")
                .post("/aws2-translate/translate")
                .then()
                .statusCode(200);
    }
}
