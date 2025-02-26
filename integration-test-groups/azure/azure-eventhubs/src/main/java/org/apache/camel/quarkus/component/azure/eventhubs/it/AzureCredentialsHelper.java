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
package org.apache.camel.quarkus.component.azure.eventhubs.it;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.azure.core.amqp.implementation.ConnectionStringProperties;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public final class AzureCredentialsHelper {
    private AzureCredentialsHelper() {
        // Utility class
    }

    public static boolean isMinimumConfigurationAvailable() {
        Config config = ConfigProvider.getConfig();
        if (isMockBackEnd()) {
            return false;
        }
        Optional<String> storageAccountName = config.getOptionalValue("azure.storage.account-name", String.class);
        Optional<String> storageAccountKey = config.getOptionalValue("azure.storage.account-key", String.class);
        Optional<String> connectionString = config.getOptionalValue("azure.event.hubs.connection.string", String.class);
        return storageAccountName.isPresent() && storageAccountKey.isPresent() && connectionString.isPresent();
    }

    public static boolean isAzureIdentityCredentialsAvailable() {
        Config config = ConfigProvider.getConfig();
        if (isMockBackEnd()) {
            return false;
        }
        Optional<Boolean> disableIdentity = config.getOptionalValue("CAMEL_QUARKUS_DISABLE_IDENTITY_EXCEPT_KEY_VAULT",
                Boolean.class);
        if (disableIdentity.isPresent() && disableIdentity.get()) {
            return false;
        }

        Optional<String> clientId = config.getOptionalValue("azure.client.id", String.class);
        Optional<String> tenantId = config.getOptionalValue("azure.tenant.id", String.class);
        Optional<String> username = config.getOptionalValue("azure.username", String.class);
        Optional<String> password = config.getOptionalValue("azure.password", String.class);
        Optional<String> clientSecret = config.getOptionalValue("azure.client.secret", String.class);
        Optional<String> clientCertificate = config.getOptionalValue("azure.client.certificate.path", String.class);
        Optional<String> clientCertificatePassword = config.getOptionalValue("azure.client.certificate.password", String.class);
        return (clientId.isPresent() && tenantId.isPresent() &&
                (username.isPresent() || password.isPresent() || clientCertificate.isPresent() || clientSecret.isPresent()
                        || clientCertificatePassword.isPresent()));
    }

    public static boolean isSharedAccessKeyAvailable() {
        Config config = ConfigProvider.getConfig();
        if (isMockBackEnd()) {
            return false;
        }
        return config.getOptionalValue("azure.event.hubs.shared.access.name", String.class).isPresent()
                && config.getOptionalValue("azure.event.hubs.shared.access.key", String.class).isPresent();
    }

    public static boolean isMockBackEnd() {
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue("camel.quarkus.start.mock.backend", Boolean.class).orElse(true);
    }

    public static Map<String, String> parseConnectionString(String connectionString) {
        Map<String, String> properties = new HashMap<>();
        ConnectionStringProperties stringProperties = new ConnectionStringProperties(connectionString);
        properties.put("Endpoint", stringProperties.getEndpoint().toString());
        properties.put("EntityPath", stringProperties.getEntityPath());
        properties.put("SharedAccessKey", stringProperties.getSharedAccessKeyName());
        properties.put("SharedAccessKeyValue", stringProperties.getSharedAccessKey());

        String host = stringProperties.getEndpoint().getHost();
        properties.put("Namespace", host.substring(0, host.indexOf('.')));

        return properties;
    }
}
