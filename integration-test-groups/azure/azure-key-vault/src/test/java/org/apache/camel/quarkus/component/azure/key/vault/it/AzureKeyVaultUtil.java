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

import io.restassured.RestAssured;

public class AzureKeyVaultUtil {

    static void deleteSecretImmediately(String secretName) {
        //we need to se identity by default, as the non-identity routes may not start
        AzureKeyVaultUtil.deleteSecretImmediately(secretName, true);
    }

    static void deleteSecretImmediately(String secretName, boolean useIdentity) {
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
    }
}
