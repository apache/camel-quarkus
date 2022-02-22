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

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.apache.camel.component.azure.storage.blob.BlobOperationsDefinition;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AzureStorageBlobRoutes extends RouteBuilder {

    public static final String BLOB_NAME = "test";

    @ConfigProperty(name = "azure.storage.account-name")
    public String azureStorageAccountName;

    @ConfigProperty(name = "azure.blob.container.name")
    public String azureBlobContainerName;

    @ConfigProperty(name = "azure.storage.account-key")
    public String azureStorageAccountKey;

    @Override
    public void configure() throws Exception {
        fromF("azure-storage-blob://%s/%s", azureStorageAccountName, azureBlobContainerName)
                .id("blob-consumer")
                .autoStartup(false)
                .to("seda:blobs");

        from("direct:create")
                .to(componentUri(BlobOperationsDefinition.uploadBlockBlob));

        from("direct:read")
                .to(componentUri(BlobOperationsDefinition.getBlob));

        from("direct:update")
                .to(componentUri(BlobOperationsDefinition.uploadBlockBlob));

        from("direct:delete")
                .to(componentUri(BlobOperationsDefinition.deleteBlob));

        from("direct:list")
                .to(componentUri(BlobOperationsDefinition.listBlobs));

        from("direct:download")
                .to(componentUri(BlobOperationsDefinition.downloadBlobToFile) + "&fileDir=target");

        from("direct:copy")
                .to(componentUri(BlobOperationsDefinition.copyBlob) + "&sourceBlobAccessKey=RAW("
                        + azureStorageAccountKey + ")");

        from("direct:downloadLink")
                .to(componentUri(BlobOperationsDefinition.downloadLink))
                .setBody().header(BlobConstants.DOWNLOAD_LINK);

        from("direct:uploadBlockBlob")
                .to(componentUri(BlobOperationsDefinition.uploadBlockBlob));

        from("direct:stageBlockBlob")
                .to(componentUri(BlobOperationsDefinition.stageBlockBlobList));

        from("direct:commitBlockBlob")
                .to(componentUri(BlobOperationsDefinition.commitBlobBlockList));

        from("direct:readBlobBlocks")
                .to(componentUri(BlobOperationsDefinition.getBlobBlockList));

        from("direct:createAppendBlob")
                .to(componentUri(BlobOperationsDefinition.createAppendBlob));

        from("direct:commitAppendBlob")
                .to(componentUri(BlobOperationsDefinition.commitAppendBlob));

        from("direct:createPageBlob")
                .to(componentUri(BlobOperationsDefinition.createPageBlob));

        from("direct:uploadPageBlob")
                .to(componentUri(BlobOperationsDefinition.uploadPageBlob));

        from("direct:resizePageBlob")
                .to(componentUri(BlobOperationsDefinition.resizePageBlob));

        from("direct:clearPageBlob")
                .to(componentUri(BlobOperationsDefinition.clearPageBlob));

        from("direct:getPageBlobRanges")
                .to(componentUri(BlobOperationsDefinition.getPageBlobRanges));

        from("direct:getChangeFeed")
                .toF(componentUri(BlobOperationsDefinition.getChangeFeed));

        from("direct:createBlobContainer")
                .to(componentUri(BlobOperationsDefinition.createBlobContainer));

        from("direct:listBlobContainers")
                .to(componentUri(BlobOperationsDefinition.listBlobContainers));

        from("direct:deleteBlobContainer")
                .to(componentUri(BlobOperationsDefinition.deleteBlobContainer));
    }

    private String componentUri(final BlobOperationsDefinition operation) {
        return String.format("azure-storage-blob://%s/%s?operation=%s&blobName=%s",
                azureStorageAccountName,
                azureBlobContainerName,
                operation.name(), BLOB_NAME);
    }
}
