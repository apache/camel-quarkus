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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureStorageDatalakeTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageDatalakeTestResource.class);

    private Path tmpFolder;

    @Override
    public Map<String, String> start() {
        String realAzureStorageAccountName = AzureStorageDatalakeUtil.getRealAccountNameFromEnv();
        String realAzureStorageAccountKey = AzureStorageDatalakeUtil.getRealAccountKeyFromEnv();

        final boolean realCredentialsProvided = realAzureStorageAccountName != null && realAzureStorageAccountKey != null;

        if (!realCredentialsProvided) {
            LOGGER.warn("Set AZURE_STORAGE_ACCOUNT_NAME and AZURE_STORAGE_ACCOUNT_KEY " +
                    "or AZURE_STORAGE_DATALAKE_ACCOUNT_NAME and AZURE_STORAGE_DATALAKE_ACCOUNT_KEY env vars.");
        } else {
            MockBackendUtils.logRealBackendUsed();
        }

        //create tmp folder (for routes)
        String tmpFolderPath = null;
        try {
            tmpFolder = Files.createTempDirectory("CqAzureDatalakeTestTmpFolder");
            tmpFolderPath = tmpFolder.toFile().getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Map.of(
                "azure.datalake.service.url",
                "https://" + realAzureStorageAccountName + ".dfs.core.windows.net",
                "cqDatalakeTmpFolder", tmpFolderPath,
                "cqCDatalakeConsumerFilesystem", "cqfsconsumer" + RandomStringUtils.secure().nextNumeric(16));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void stop() {
        if (tmpFolder != null && tmpFolder.toFile().exists()) {
            try (var dirStream = Files.walk(tmpFolder)) {
                dirStream
                        .map(Path::toFile)
                        .sorted(Comparator.reverseOrder())
                        .forEach(File::delete);
            } catch (IOException e) {
                //nothing
            }
        }
    }
}
