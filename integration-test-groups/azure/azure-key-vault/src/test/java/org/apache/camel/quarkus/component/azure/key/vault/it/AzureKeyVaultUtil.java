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

import java.util.Map;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import io.restassured.RestAssured;
import org.jboss.logging.Logger;

public class AzureKeyVaultUtil {
    private static final Logger LOG = Logger.getLogger(AzureKeyVaultUtil.class);

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_MS = 5000; // 5 seconds

    static void deleteSecretImmediately(String secretName, boolean useIdentity) {

        boolean deleted = false;

        try {
            // Delete secret
            RestAssured.given()
                    .delete("/azure-key-vault/secret/" + useIdentity + "/{secretName}", secretName)
                    .then()
                    .statusCode(200);

            // Purge secret
            RestAssured.given()
                    .delete("/azure-key-vault/secret/" + useIdentity + "/{secretName}/purge", secretName)
                    .then()
                    .statusCode(200);

            // Confirm deletion
            RestAssured.given()
                    .queryParam("identity", useIdentity)
                    .get("/azure-key-vault/secret/" + useIdentity + "/{secretName}", secretName)
                    .then()
                    .statusCode(500);
            deleted = true;
        } finally {
            if (!deleted) {
                // in case the deletion via component fails, delete directly via client
                deleteSecretImmediatelyViaClient(secretName);
            }
        }

    }

    private static void deleteSecretImmediatelyViaClient(String secretName) {

        //create client
        String keyVaultUri = "https://" + System.getenv("AZURE_VAULT_NAME") + ".vault.azure.net";
        TokenCredential credential = ((ClientSecretCredentialBuilder) ((ClientSecretCredentialBuilder) (new ClientSecretCredentialBuilder())
                .tenantId(System.getenv("AZURE_TENANT_ID"))).clientId(System.getenv("AZURE_CLIENT_ID")))
                .clientSecret(System.getenv("AZURE_CLIENT_SECRET")).build();

        SecretClient client = (new SecretClientBuilder()).vaultUrl(keyVaultUri).credential(credential).buildClient();

        try {
            KeyVaultSecret secret = client.getSecret(secretName);

            if (secret != null) {
                client.beginDeleteSecret(secretName);
            }

        } catch (ResourceNotFoundException e) {
            //already deleted
        } finally {
            //purge secret in all cases to be sure it is purged
            try {
                client.purgeDeletedSecret(secretName);
            } catch (HttpResponseException e) {
                if (e.getResponse().getStatusCode() == 409) { // Conflict: Object is being deleted
                    int attempt = 0;
                    while (attempt++ < MAX_RETRIES) {
                        LOG.infof("Attempt %d to delete secret '%s'.", attempt, secretName);
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ex) {
                            LOG.errorf("Purging of secret `%s` failed", secretName, ex);
                        }
                        try {
                            client.purgeDeletedSecret(secretName);
                            break;
                        } catch (HttpResponseException ex) {
                            LOG.errorf("Purging of secret `%s` failed", secretName, ex);
                        }

                    }
                    if (attempt >= MAX_RETRIES) {
                        LOG.errorf("Purging of secret `%s` failed after %d attempts.", secretName, attempt);
                    }
                }
            }
        }
    }

    public static void setPropertyIfEnvVarPresent(Map<String, String> properties, String key, String envVarName) {
        if (System.getenv(envVarName) != null) {
            properties.put(key, System.getenv(envVarName));
        }
    }
}
