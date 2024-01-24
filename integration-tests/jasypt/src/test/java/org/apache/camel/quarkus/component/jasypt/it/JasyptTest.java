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
package org.apache.camel.quarkus.component.jasypt.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class JasyptTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "direct:decryptConfiguration",
            "direct:decryptConfigurationFromXml",
            "direct:decryptConfigurationFromYaml"
    })
    void decryptSimpleConfigPropertyPlaceholder(String endpointURI) {
        RestAssured.given()
                .queryParam("endpointURI", endpointURI)
                .get("/jasypt/decrypt/configuration/greeting.secret")
                .then()
                .statusCode(200)
                .body(is("Hello World"));
    }

    @Test
    void decryptSimpleConfigPropertyPlaceholderWithExpression() {
        RestAssured.given()
                .queryParam("endpointURI", "direct:decryptConfiguration")
                .get("/jasypt/decrypt/configuration/greeting.expression.secret")
                .then()
                .statusCode(200)
                .body(is("Hello World From Expression"));
    }

    @Test
    void decryptSimpleConfigPropertyPlaceholderWithExplicitConfigProvider() {
        RestAssured.given()
                .queryParam("endpointURI", "direct:decryptConfiguration")
                .get("/jasypt/decrypt/configuration/explicit.config.provider.secret")
                .then()
                .statusCode(200)
                .body(is("Hello World"));
    }

    @Test
    void decryptInjectedConfigProperty() {
        RestAssured.get("/jasypt/decrypt/injected/configuration/direct:secretPropertyInjection")
                .then()
                .statusCode(200)
                .body(is("Hello World"));
    }

    @Test
    void decryptInjectedConfigPropertyWithExplicitConfigProvider() {
        RestAssured.get("/jasypt/decrypt/injected/configuration/direct:secretExplicitConfigProviderPropertyInjection")
                .then()
                .statusCode(200)
                .body(is("Hello World"));
    }

    @Test
    void timerConfiguredWithEncryptedPropertiesFired() {
        RestAssured.given()
                .queryParam("expectedMessageCount", 2)
                .get("/jasypt/timer/mock/results")
                .then()
                .statusCode(204);
    }

    @Test
    void resolveInsecureProperty() {
        RestAssured.given()
                .queryParam("endpointURI", "direct:decryptConfiguration")
                .get("/jasypt/decrypt/configuration/insecure.property")
                .then()
                .statusCode(200)
                .body(is("Hello World"));
    }
}
