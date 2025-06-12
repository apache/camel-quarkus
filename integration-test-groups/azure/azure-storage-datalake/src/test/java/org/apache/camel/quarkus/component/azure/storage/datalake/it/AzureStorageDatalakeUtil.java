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

public class AzureStorageDatalakeUtil {

    public static String getRealAccountNameFromEnv() {
        return Optional.ofNullable(System.getenv("AZURE_STORAGE_DATALAKE_ACCOUNT_NAME"))
                .orElseGet(() -> System.getenv("AZURE_STORAGE_ACCOUNT_NAME"));
    }

    public static String getRealAccountKeyFromEnv() {
        return Optional.ofNullable(System.getenv("AZURE_STORAGE_DATALAKE_ACCOUNT_KEY"))
                .orElseGet(() -> System.getenv("AZURE_STORAGE_ACCOUNT_KEY"));
    }

    public static boolean isRalAccountProvided() {
        String realAzureStorageAccountName = AzureStorageDatalakeUtil.getRealAccountNameFromEnv();
        String realAzureStorageAccountKey = AzureStorageDatalakeUtil.getRealAccountKeyFromEnv();

        return realAzureStorageAccountName != null && realAzureStorageAccountKey != null;
    }
}
