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
package org.apache.camel.quarkus.component.azure.key.vault.it;

import java.util.UUID;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

// Azure Key Vault is not supported by Azurite https://github.com/Azure/Azurite/issues/619
abstract class AbstractAzureKeyVaultTest {

    private final boolean useIdentity;

    public AbstractAzureKeyVaultTest(boolean useIdentity) {
        this.useIdentity = useIdentity;
    }

    @BeforeEach
    public void beforeEach() {
        //routes without identity have to be started
        if (!useIdentity) {
            RestAssured.given()
                    .post("/azure-key-vault/secret/routes/start")
                    .then()
                    .statusCode(204);
        }
    }

    @AfterEach
    public void afterEach() {
        //routes without identity have to be stopped
        if (!useIdentity) {
            RestAssured.given()
                    .post("/azure-key-vault/secret/routes/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void secretCreateRetrieveDeletePurge() {
        String secretName = "cq-create" + (useIdentity ? "-identity-" : "-") + UUID.randomUUID().toString();
        String secret = "Hello Camel Quarkus Azure Key Vault";

        try {
            // Create secret
            RestAssured.given()
                    .body(secret)
                    .post("/azure-key-vault/secret/" + useIdentity + "/{secretName}", secretName)
                    .then()
                    .statusCode(200)
                    .body(is(secretName));

            // Retrieve secret
            RestAssured.given()
                    .get("/azure-key-vault/secret/" + useIdentity + "/{secretName}", secretName)
                    .then()
                    .statusCode(200)
                    .body(is(secret));
        } finally {
            AzureKeyVaultUtil.deleteSecretImmediately(secretName, useIdentity);
        }
    }

}
