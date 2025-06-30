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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.azure.storage.file.datalake.models.ListFileSystemsOptions;
import com.azure.storage.file.datalake.options.FileQueryOptions;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.datalake.DataLakeConstants;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class AzureStorageDatalakeRoutes extends RouteBuilder {

    public static final String FILE_NAME = "operations.txt";
    public static final String FILE_NAME2 = "test/file.txt";
    public static final String CONSUMER_FILE_NAME = "file_for_download.txt";
    public static final String CONSUMER_FILE_NAME2 = "file_for_download2.txt";
    private static final String CLIENT_SUFFIX = "&serviceClient=#azureDatalakeServiceClient";

    @Override
    public void configure() throws Exception {

        if (AzureStorageDatalakeUtil.isRalAccountProvided()) {

            String tmpFolder = ConfigProvider.getConfig().getValue("cqDatalakeTmpFolder", String.class);
            String consumerFilesystem = ConfigProvider.getConfig().getValue("cqCDatalakeConsumerFilesystem", String.class);

            /* Consumer examples */

            //Consume a file from the storage datalake into a file using the file component
            from("azure-storage-datalake://" + AzureStorageDatalakeUtil.getRealAccountKeyFromEnv() + "/" + consumerFilesystem
                    + "?fileName=" + CONSUMER_FILE_NAME)
                    .routeId("consumeWithFileComponent")
                    .autoStartup(false)
                    .to("file:" + tmpFolder + "/consumer-files?fileName=" + CONSUMER_FILE_NAME);

            //write to a file without using the file component
            from("azure-storage-datalake://" + AzureStorageDatalakeUtil.getRealAccountKeyFromEnv() + "/" + consumerFilesystem
                    + "?fileName=" + CONSUMER_FILE_NAME2 + "&fileDir=" + tmpFolder + "/consumer-files&delay=3000000")
                    .routeId("consumeWithoutFileComponent")
                    .autoStartup(false)
                    .log("File downloaded");

            //batch consumer
            from("azure-storage-datalake://" + AzureStorageDatalakeUtil.getRealAccountKeyFromEnv() + "/" + consumerFilesystem
                    + "?fileDir=" + tmpFolder + "/consumer-files/batch&path=/&delay=3000000")
                    .routeId("consumeBatch")
                    .autoStartup(false)
                    .log("File downloaded");

            /* Producer examples */

            //listFileSystem
            from("direct:datalakeListFileSystem")
                    .process(exchange -> {
                        exchange.getIn().setHeader(DataLakeConstants.LIST_FILESYSTEMS_OPTIONS,
                                new ListFileSystemsOptions().setMaxResultsPerPage(10));
                    })
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=listFileSystem");

            //createFileSystem
            from("direct:datalakeCreateFilesystem")
                    .toD("azure-storage-datalake://${header.accountName}?operation=createFileSystem");

            //listPaths
            from("direct:datalakeListPaths")
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=listPaths");

            //getFile
            from("direct:datalakeGetFile")
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=getFile&fileName=${header.fileName}");

            //deleteFile
            from("direct:datalakeDeleteFile")
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=deleteFile&fileName="
                            + FILE_NAME);

            //downloadToFile
            from("direct:datalakeDownloadToFile")
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=downloadToFile&fileName="
                            + FILE_NAME + "&fileDir=${header.tmpFolder}");

            //downloadLink
            from("direct:datalakeDownloadLink")
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=downloadLink&fileName="
                            + FILE_NAME);

            //appendToFile
            from("direct:datalakeAppendToFile")
                    .process(exchange -> {
                        final String data = exchange.getIn().getHeader("append", String.class);
                        final InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
                        exchange.getIn().setBody(inputStream);
                    })
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=appendToFile&fileName="
                            + FILE_NAME);

            //flushToFile
            from("direct:datalakeFlushToFile")
                    .process(exchange -> {
                        exchange.getIn().setHeader(DataLakeConstants.POSITION, 8);
                    })
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=flushToFile&fileName="
                            + FILE_NAME);

            //openQueryInputStream
            from("direct:openQueryInputStream")
                    .process(exchange -> {
                        exchange.getIn().setHeader(DataLakeConstants.QUERY_OPTIONS,
                                new FileQueryOptions("SELECT * from BlobStorage"));
                    })
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=openQueryInputStream&fileName="
                            + FILE_NAME);

            //upload
            from("direct:datalakeUpload")
                    .process(exchange -> {
                        String fileContent = exchange.getIn().getHeader("fileContent", String.class);
                        final InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
                        exchange.getIn().setBody(inputStream);
                    })
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=upload&fileName="
                            + FILE_NAME);

            // uploadFromFile
            from("direct:datalakeUploadFromFile")
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=uploadFromFile&fileName="
                            + FILE_NAME2);

            // createFile
            from("direct:datalakeCreateFile")
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=createFile&fileName=${header.fileName}");

            //deleteDirectory
            from("direct:datalakeDeleteDirectory")
                    .toD("azure-storage-datalake://${header.accountName}/${header.filesystemName}?operation=deleteDirectory");

            //listFileSystem

            /* Authentications */

            //SAS list paths
            from("direct:datalakeSasListPaths")
                    .toD("azureDatalakeSasComponent://${header.accountName}/${header.filesystemName}?operation=listPaths");
            //client instance list paths
            from("direct:datalakeClientInstanceListPaths")
                    .toD("azureDatalakeClientInstanceComponent://${header.accountName}/${header.filesystemName}?operation=listPaths"
                            + CLIENT_SUFFIX);
            //SHARED_KEY_CREDENTIAL
            from("direct:datalakeSharedKeyCredentialsListPaths")
                    .toD("azureDatalakeSharedkeyCredentialComponent://${header.accountName}/${header.filesystemName}?operation=listPaths");
            //client secret
            from("direct:datalakeClientSecretListPaths")
                    .toD("azureDatalakeClientSecretComponent://${header.accountName}/${header.filesystemName}?operation=listPaths");
        }
    }
}
