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

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.ws.rs.Produces;
import org.apache.camel.component.azure.storage.datalake.CredentialType;
import org.apache.camel.component.azure.storage.datalake.DataLakeComponent;
import org.apache.camel.component.azure.storage.datalake.DataLakeConfiguration;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AzureStorageDatalakeProducers {

    @ConfigProperty(name = "azure.storage.account-name")
    Optional<String> azureStorageAccountName;

    @ConfigProperty(name = "azure.storage.account-key")
    Optional<String> azureStorageAccountKey;

    @ConfigProperty(name = "azure.datalake.service.url")
    Optional<String> serviceUrl;

    @jakarta.enterprise.inject.Produces
    @Named("azureDatalakeServiceClient")
    public DataLakeServiceClient createDatalakeServiceClient() throws Exception {
        StorageSharedKeyCredential credentials = new StorageSharedKeyCredential(azureStorageAccountName.get(),
                azureStorageAccountKey.get());
        return new DataLakeServiceClientBuilder()
                .endpoint(serviceUrl.get())
                .credential(credentials)
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .buildClient();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @jakarta.enterprise.inject.Produces
    @Named("azureDatalakeSasComponent")
    public DataLakeComponent azureDatalakeSasComponent() throws Exception {
        if (!AzureStorageDatalakeUtil.isSasTokenProvided()) {
            return null;
        }

        DataLakeComponent dc = new DataLakeComponent();
        dc.setAutowiredEnabled(false);

        DataLakeConfiguration configuration = new DataLakeConfiguration();
        configuration.setCredentialType(CredentialType.AZURE_SAS);
        configuration.setSasSignature(AzureStorageDatalakeUtil.getSasToken().get());
        dc.setConfiguration(configuration);

        return dc;
    }

    @jakarta.enterprise.inject.Produces
    @Named("azureDatalakeClientInstanceComponent")
    public DataLakeComponent azureDatalakeClientInstanceComponent() throws Exception {
        DataLakeComponent dc = new DataLakeComponent();

        DataLakeConfiguration configuration = new DataLakeConfiguration();
        configuration.setCredentialType(CredentialType.CLIENT_SECRET);
        dc.setConfiguration(configuration);

        return dc;
    }

    @jakarta.enterprise.inject.Produces
    @Named("azureDatalakeSharedkeyCredentialComponent")
    public DataLakeComponent azureDatalakeSharedkeyCredentialComponent() throws Exception {
        DataLakeComponent dc = new DataLakeComponent();
        dc.setAutowiredEnabled(false);

        DataLakeConfiguration configuration = new DataLakeConfiguration();
        configuration.setCredentialType(CredentialType.SHARED_KEY_CREDENTIAL);
        configuration.setAccountName(AzureStorageDatalakeUtil.getRealAccountNameFromEnv());
        configuration.setAccountKey(AzureStorageDatalakeUtil.getRealAccountKeyFromEnv());
        dc.setConfiguration(configuration);

        return dc;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @jakarta.enterprise.inject.Produces
    @Named("azureDatalakeClientSecretComponent")
    public DataLakeComponent azureDatalakeClientSecretComponent() throws Exception {
        if (!AzureStorageDatalakeUtil.isRealClientSecretProvided()) {
            return null;
        }

        DataLakeComponent dc = new DataLakeComponent();
        dc.setAutowiredEnabled(false);

        DataLakeConfiguration configuration = new DataLakeConfiguration();
        configuration.setCredentialType(CredentialType.CLIENT_SECRET);
        configuration.setClientId(AzureStorageDatalakeUtil.getClientId().get());
        configuration.setClientSecret(AzureStorageDatalakeUtil.getClientSecret().get());
        configuration.setTenantId(AzureStorageDatalakeUtil.getTenantId().get());
        dc.setConfiguration(configuration);

        return dc;
    }

}
