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
package org.apache.camel.quarkus.component.jolokia.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.camel.builder.Builder.body;
import static org.hamcrest.Matchers.equalTo;

@TestProfile(JolokiaCustomRestrictorTest.JolokiaCustomRestrictorProfile.class)
@QuarkusTest
class JolokiaCustomRestrictorTest {
    @BeforeEach
    public void beforeEach() {
        RestAssured.port = 8778;
    }

    @Test
    void customRestrictorDeniesOperationAccess() {
        // sendStringBody operations should be allowed
        String sendStringBodyPayload = "{\"type\":\"exec\",\"mbean\":\"org.apache.camel:context=camel-1,type=context,name=\\\"camel-1\\\"\",\"operation\":\"sendStringBody(java.lang.String, java.lang.String)\",\"arguments\":[\"direct://start\",\"Hello World\"]}";
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(sendStringBodyPayload)
                .post("/jolokia/")
                .then()
                .statusCode(200)
                .body("status", equalTo(200));

        // All other operations should be disallowed
        String dumpRoutesAsXmlPayload = "{\"type\":\"exec\",\"mbean\":\"org.apache.camel:context=camel-1,type=context,name=\\\"camel-1\\\"\",\"operation\":\"dumpRoutesAsXml()\",\"arguments\":[]}";
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(dumpRoutesAsXmlPayload)
                .post("/jolokia/")
                .then()
                .statusCode(200)
                .body("status", equalTo(403));
    }

    public static final class JolokiaCustomRestrictorProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.camel.jolokia.additional-properties.restrictorClass", CustomRestrictor.class.getName());
        }
    }
}
