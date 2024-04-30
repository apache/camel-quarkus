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
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(JasyptSecureExtensionConfigTestProfile.class)
class JasyptSecureExtensionConfigTest {
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "https://github.com/apache/camel-quarkus/issues/5675")
    @Test
    void secureDirectComponentTimeout() throws InterruptedException {
        RestAssured.given()
                .get("/jasypt/secure/direct/component")
                .then()
                .statusCode(200)
                .body(is("60000"));
    }
}
