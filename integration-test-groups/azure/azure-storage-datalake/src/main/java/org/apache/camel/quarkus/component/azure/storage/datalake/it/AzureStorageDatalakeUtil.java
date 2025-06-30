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
package org.apache.camel.quarkus.component.azure.storage.datalake.it;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

public class AzureStorageDatalakeUtil {

    public static String getRealAccountNameFromEnv() {
        return Optional.ofNullable(System.getenv("AZURE_STORAGE_DATALAKE_ACCOUNT_NAME"))
                .orElseGet(() -> System.getenv("AZURE_STORAGE_ACCOUNT_NAME"));
    }

    public static String getRealAccountKeyFromEnv() {
        return Optional.ofNullable(System.getenv("AZURE_STORAGE_DATALAKE_ACCOUNT_KEY"))
                .orElseGet(() -> System.getenv("AZURE_STORAGE_ACCOUNT_KEY"));
    }

    public static Optional<String> getSasToken() {
        return getDatalakePrioritizedConfigValue("azure.storage.datalake.sas", "azure.storage.sas", String.class);
    }

    public static Optional<String> getClientId() {
        return getDatalakePrioritizedConfigValue("azure.datalake.client.id", "azure.client.id", String.class,
                disableIdentityExceptKeyVault());
    }

    public static Optional<String> getClientSecret() {
        return getDatalakePrioritizedConfigValue("azure.datalake.client.secret", "azure.client.secret", String.class,
                disableIdentityExceptKeyVault());
    }

    public static Optional<String> getTenantId() {
        return getDatalakePrioritizedConfigValue("azure.datalake.tenant.id", "azure.tenant.id", String.class,
                disableIdentityExceptKeyVault());
    }

    public static boolean disableIdentityExceptKeyVault() {
        return ConfigProvider.getConfig().getOptionalValue("CAMEL_QUARKUS_DISABLE_IDENTITY_EXCEPT_KEY_VAULT", Boolean.class)
                .orElse(false);
    }

    public static boolean isRalAccountProvided() {
        String realAzureStorageAccountName = AzureStorageDatalakeUtil.getRealAccountNameFromEnv();
        String realAzureStorageAccountKey = AzureStorageDatalakeUtil.getRealAccountKeyFromEnv();

        return realAzureStorageAccountName != null && realAzureStorageAccountKey != null;
    }

    public static boolean isRealClientSecretProvided() {
        return getClientId().isPresent()
                && getClientSecret().isPresent()
                && getTenantId().isPresent();
    }

    public static boolean isSasTokenProvided() {
        return getSasToken().isPresent();
    }

    private static <T> Optional<T> getDatalakePrioritizedConfigValue(String primaryName, String secondaryNajme, Class<T> type) {
        return getDatalakePrioritizedConfigValue(primaryName, secondaryNajme, type, false);
    }

    private static <T> Optional<T> getDatalakePrioritizedConfigValue(String primaryName, String secondaryNajme, Class<T> type,
            boolean ignoreSecondary) {
        Optional<T> value = ConfigProvider.getConfig().getOptionalValue(primaryName, type);
        if (value.isEmpty() && !ignoreSecondary) {
            value = ConfigProvider.getConfig().getOptionalValue(secondaryNajme, type);
        }
        return value;
    }
}
