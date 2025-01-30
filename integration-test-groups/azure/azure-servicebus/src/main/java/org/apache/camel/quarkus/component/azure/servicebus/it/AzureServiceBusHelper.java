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
package org.apache.camel.quarkus.component.azure.servicebus.it;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public final class AzureServiceBusHelper {
    private AzureServiceBusHelper() {
        // Utility class
    }

    public static boolean isMinimumConfigurationAvailable() {
        Config config = ConfigProvider.getConfig();
        Optional<String> connectionString = config.getOptionalValue("azure.servicebus.connection.string",
                String.class);
        Optional<String> queueName = config.getOptionalValue("azure.servicebus.queue.name", String.class);
        return connectionString.isPresent() && queueName.isPresent();
    }

    public static boolean isAzureIdentityCredentialsAvailable() {
        Config config = ConfigProvider.getConfig();
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

    public static boolean isAzureServiceBusTopicConfigPresent() {
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue("azure.servicebus.topic.name", String.class).isPresent()
                && config.getOptionalValues("azure.servicebus.topic.subscription.name", String.class).isPresent();
    }

    public static String getDestination(String type) {
        return ConfigProvider.getConfig().getValue("azure.servicebus.%s.name".formatted(type), String.class);
    }

    public static String getConnectionString() {
        return ConfigProvider.getConfig().getValue("azure.servicebus.connection.string", String.class);
    }

    public static boolean isMockBackEnd() {
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue("camel.quarkus.start.mock.backend", Boolean.class).orElse(true);
    }
}
