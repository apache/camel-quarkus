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
package org.apache.camel.quarkus.component.qdrant.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@TestProfile(QdrantAuthTestProfile.class)
class QdrantAuthTest {

    @Test
    void apiKeyAuthenticationShouldWork() {
        // This test runs with QdrantAuthTestProfile which configures the component with API key
        RestAssured.put("/qdrant/apiKey/valid")
                .then()
                .statusCode(200)
                .body(containsString("ApiKeyCredentials reflection works"));
    }

    @Test
    void invalidApiKeyShouldBeRejected() {
        // Verify that authentication is actually enforced with wrong API key
        RestAssured.put("/qdrant/apiKey/invalid")
                .then()
                .statusCode(403)
                .body(containsString("Authentication correctly failed"));
    }

}
