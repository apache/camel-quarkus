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

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// Azure Key Vault is not supported by Azurite https://github.com/Azure/Azurite/issues/619
@EnabledIfEnvironmentVariable(named = "AZURE_TENANT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_CLIENT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_CLIENT_SECRET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_VAULT_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_STORAGE_ACCOUNT_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_VAULT_EVENT_HUBS_CONNECTION_STRING", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_VAULT_EVENT_HUBS_BLOB_CONTAINER_NAME", matches = ".+")
@QuarkusIntegrationTest
class AzureKeyVaultContextReloadIT extends AzureKeyVaultContextReloadTest {

}
