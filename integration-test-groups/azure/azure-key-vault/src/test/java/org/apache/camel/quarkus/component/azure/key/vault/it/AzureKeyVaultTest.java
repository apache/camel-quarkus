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

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.hamcrest.Matchers.is;

/**
 * Test for key vault create/delete/purge with the `credentialType=CLIENT_SECRET`
 * </br>
 * Requires own test profile, which sets credentials to the vault configuration.
 */
// Azure Key Vault is not supported by Azurite https://github.com/Azure/Azurite/issues/619
@EnabledIfEnvironmentVariable(named = "AZURE_TENANT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_CLIENT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_CLIENT_SECRET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_VAULT_NAME", matches = ".+")
@TestProfile(AzureKeyVaultTestProfile.class)
@QuarkusTest
class AzureKeyVaultTest extends AbstractAzureKeyVaultTest {

    public AzureKeyVaultTest() {
        super(false);
    }

    /**
     * Creation of the secret with the client without identity or clientSecret should fail.
     */
    @Test
    void wrongClientTest() {
        String secretName = "cq-create-with-identity" + UUID.randomUUID().toString();
        String secret = "Hello Camel Quarkus Azure Key Vault";
        boolean tryToDeleteSecret = true;
        try {
            // Create secret
            RestAssured.given()
                    .body(secret)
                    .queryParam("suffix", "Wrong")
                    .post("/azure-key-vault/secret/wrongClient/{secretName}", secretName)
                    .then()
                    .statusCode(500)
                    .body(is("ResolveEndpointFailedException"));

            //don't delete secret as it was not created
            tryToDeleteSecret = false;
        } finally {
            if (tryToDeleteSecret) {
                AzureKeyVaultUtil.deleteSecretImmediately(secretName, true);
            }
        }
    }
}
