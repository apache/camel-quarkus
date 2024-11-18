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
package org.apache.camel.quarkus.component.google.secret.manager.it;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.secretmanager.v1.ProjectName;
import com.google.cloud.secretmanager.v1.Replication;
import com.google.cloud.secretmanager.v1.Secret;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretManagerServiceSettings;
import com.google.cloud.secretmanager.v1.SecretName;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.protobuf.ByteString;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleSecretManagerTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleSecretManagerTestResource.class);

    private String gcpSecretId;
    private String accessFile;
    private String projectName;
    private boolean realBackendIsUsed = false;

    @Override
    public Map<String, String> start() {

        gcpSecretId = "CQ-GCPTestSecret" + System.currentTimeMillis();
        String gcpSecretValue = "GCP secret value";

        accessFile = System.getenv("GOOGLE_SERVICE_ACCOUNT_KEY");
        projectName = System.getenv("GOOGLE_PROJECT_NAME");

        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
        final boolean realCredentialsProvided = accessFile != null && !accessFile.isEmpty() && projectName != null
                && !projectName.isEmpty();
        final boolean usingMockBackend = startMockBackend && !realCredentialsProvided;

        if (usingMockBackend) {
            //try wiremock
            throw new RuntimeException("Mocked test backend is not implemented yet");
        } else {
            if (!startMockBackend && !realCredentialsProvided) {
                throw new IllegalStateException(
                        "Set GOOGLE_PROJECT and GOOGLE_SERVICE_ACCOUNT_KEY env vars if you set CAMEL_QUARKUS_START_MOCK_BACKEND=false");
            }
            MockBackendUtils.logRealBackendUsed();
            realBackendIsUsed = true;
            //create secret for gcp
            createSecret(gcpSecretId, gcpSecretValue, accessFile, projectName);
        }
        return Map.of("gcpSecretId", gcpSecretId, "gcpSecretValue", gcpSecretValue);
    }

    static void createSecret(String secretId, String secretValue, String accessFile, String project) {
        SecretManagerServiceClient client = null;
        try (FileInputStream fis = new FileInputStream(accessFile)) {

            Credentials myCredentials = ServiceAccountCredentials.fromStream(fis);
            SecretManagerServiceSettings settings = SecretManagerServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials)).build();
            client = SecretManagerServiceClient.create(settings);

            Secret secret = Secret.newBuilder()
                    .setReplication(
                            Replication.newBuilder()
                                    .setAutomatic(Replication.Automatic.newBuilder().build())
                                    .build())
                    .build();

            Secret createdSecret = client.createSecret(ProjectName.of(project), secretId, secret);

            SecretPayload payload = SecretPayload.newBuilder()
                    .setData(ByteString.copyFromUtf8(secretValue)).build();

            client.addSecretVersion(createdSecret.getName(), payload);

        } catch (IOException e) {
            LOG.error("Unsuccessful creation of secret (%s) for gcp. ".formatted(secretId));
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    static void deleteSecret(String secretId, String accessFile, String project) {

        SecretManagerServiceClient client = null;
        try (FileInputStream fis = new FileInputStream(accessFile)) {

            Credentials myCredentials = ServiceAccountCredentials.fromStream(fis);
            SecretManagerServiceSettings settings = SecretManagerServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials)).build();
            client = SecretManagerServiceClient.create(settings);

            client.deleteSecret(SecretName.of(project, secretId));
        } catch (IOException e) {
            LOG.error("Unsuccessful creation of secret (%s) for gcp. ".formatted(secretId));
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Override
    public void stop() {

        if (realBackendIsUsed) {
            deleteSecret(gcpSecretId, accessFile, projectName);
        }
    }
}
