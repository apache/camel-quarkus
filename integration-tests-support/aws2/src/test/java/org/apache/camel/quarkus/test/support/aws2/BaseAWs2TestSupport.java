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
package org.apache.camel.quarkus.test.support.aws2;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Parent for aws test classes which should cover default credentials test case.
 * Parent adds two test methods {@link #successfulDefaultCredentialsProviderTest()} and
 * {@link #failingDefaultCredentialsProviderTest()}
 *
 * Test expects that test resource extends {@link BaseAws2Resource}.
 *
 */
public abstract class BaseAWs2TestSupport {

    //Rest path to connect to rest resource. Path differs for different aws2 extensions (like "/aws2-ddb" or "/aws2-s3")
    private final String restPath;

    public BaseAWs2TestSupport(String restPath) {
        this.restPath = restPath;
    }

    /**
     * Testing method used for {@link #successfulDefaultCredentialsProviderTest()} and
     * {@link #failingDefaultCredentialsProviderTest()}.
     *
     * This method is called twice.
     * 1 - Credentials are not set, therefore this method should fail.
     * 2 - Credentials are set, there this method should succeed.
     *
     * Returns true if test passes, fa;se otherwise.
     */
    public abstract void testMethodForDefaultCredentialsProvider();

    //test can be executed only if mock backend is used and no defaultCredentialsProvider is defined in the system
    @ExtendWith(Aws2DefaultCredentialsProviderAvailabilityCondition.class)
    @Test
    public void successfulDefaultCredentialsProviderTest() {
        try {
            RestAssured.given()
                    .body(true)
                    .post(restPath + "/setUseDefaultCredentialsProvider")
                    .then()
                    .statusCode(200);

            RestAssured.given()
                    .body(true)
                    .post(restPath + "/initializeDefaultCredentials")
                    .then()
                    .statusCode(200);

            //should succeed
            testMethodForDefaultCredentialsProvider();

        } finally {
            RestAssured.given()
                    .body(false)
                    .post(restPath + "/initializeDefaultCredentials")
                    .then()
                    .statusCode(200);

            RestAssured.given()
                    .body(false)
                    .post(restPath + "/setUseDefaultCredentialsProvider")
                    .then()
                    .statusCode(200);
        }
    }

    //test can be executed only if mock backend is used and no defaultCredentialsprovider is defined in the system
    @ExtendWith(Aws2DefaultCredentialsProviderAvailabilityCondition.class)
    @Test
    public void failingDefaultCredentialsProviderTest() {
        RestAssured.given()
                .body(true)
                .post(restPath + "/setUseDefaultCredentialsProvider")
                .then()
                .statusCode(200);

        // should fail without credentials for aws
        Assertions.assertThrows(AssertionError.class, () -> testMethodForDefaultCredentialsProvider());

        RestAssured.given()
                .body(false)
                .post(restPath + "/setUseDefaultCredentialsProvider")
                .then()
                .statusCode(200);
    }

}
