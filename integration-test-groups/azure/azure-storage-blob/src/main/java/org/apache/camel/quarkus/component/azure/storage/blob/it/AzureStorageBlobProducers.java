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
package org.apache.camel.quarkus.component.azure.storage.blob.it;

import java.io.IOException;

import javax.inject.Named;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.apache.camel.component.azure.storage.blob.BlobComponent;
import org.apache.camel.component.azure.storage.blob.BlobConfiguration;
import org.apache.camel.component.azure.storage.blob.CredentialType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class AzureStorageBlobProducers {

    @ConfigProperty(name = "azure.storage.account-name")
    String azureStorageAccountName;

    @ConfigProperty(name = "azure.storage.account-key")
    String azureStorageAccountKey;

    @ConfigProperty(name = "azure.blob.service.url")
    String azureBlobServiceUrl;

    @Named("azureStorageSharedKeyCredential")
    public StorageSharedKeyCredential azureStorageSharedKeyCredential() {
        return new StorageSharedKeyCredential(azureStorageAccountName, azureStorageAccountKey);
    }

    @Named("azureBlobServiceClient")
    public BlobServiceClient createBlobClient(StorageSharedKeyCredential credential) {
        return getBlobClientBuilder()
                .credential(credential)
                .buildClient();
    }

    @Named("azure-storage-blob-managed-client")
    public BlobComponent azureBlobComponentWithManagedClient() {
        BlobConfiguration configuration = new BlobConfiguration();
        configuration.setCredentialType(CredentialType.SHARED_KEY_CREDENTIAL);

        BlobComponent component = new BlobComponent();
        component.setAutowiredEnabled(false);
        component.setConfiguration(configuration);
        return component;
    }

    @Named("azure-storage-blob-client-secret-auth")
    public BlobComponent azureStorageBlobClientSecretAuth() {
        if (AzureStorageHelper.isClientSecretAuthEnabled()) {
            BlobConfiguration configuration = new BlobConfiguration();
            configuration.setCredentialType(CredentialType.AZURE_IDENTITY);

            BlobComponent component = new BlobComponent();
            component.setAutowiredEnabled(false);
            component.setConfiguration(configuration);
            return component;
        }
        return null;
    }

    @Named("azure-storage-blob-client-certificate-auth")
    public BlobComponent azureStorageBlobClientCertificateAuth() throws IOException {
        if (AzureStorageHelper.isClientCertificateAuthEnabled()) {
            BlobConfiguration configuration = new BlobConfiguration();
            configuration.setCredentialType(CredentialType.AZURE_IDENTITY);

            BlobComponent component = new BlobComponent();
            component.setAutowiredEnabled(false);
            component.setConfiguration(configuration);
            return component;
        }
        return null;
    }

    private BlobServiceClientBuilder getBlobClientBuilder() {
        return new BlobServiceClientBuilder().endpoint(azureBlobServiceUrl)
                .httpLogOptions(new HttpLogOptions()
                        .setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS)
                        .setPrettyPrintBody(true));
    }
}
