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
package org.apache.camel.quarkus.component.salesforce;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class SalesforceTestResource extends WireMockTestResourceLifecycleManager {
    private static final String SALESFORCE_BASE_URL = "login.salesforce.com";
    private static final String SALESFORCE_CLIENT_ID = "SALESFORCE_CLIENTID";
    private static final String SALESFORCE_CLIENT_SECRET = "SALESFORCE_CLIENTSECRET";
    private static final String SALESFORCE_USERNAME = "SALESFORCE_USERNAME";
    private static final String SALESFORCE_PASSWORD = "SALESFORCE_PASSWORD";

    private Server pubSubServer;

    /**
     * Whether the tests run against mocked Salesforce APIs, which is the case unless Salesforce credentials are provided
     */
    public static boolean isMockBackend() {
        return Stream.of(SALESFORCE_CLIENT_ID, SALESFORCE_CLIENT_SECRET, SALESFORCE_USERNAME, SALESFORCE_PASSWORD)
                .anyMatch(envVar -> System.getenv(envVar) == null);
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return SALESFORCE_BASE_URL;
    }

    @Override
    protected boolean isMockingEnabled() {
        return isMockBackend();
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> options = super.start();
        if (options.containsKey("wiremock.url")) {
            options.put("camel.component.salesforce.loginUrl", options.get("wiremock.url"));
            options.put("camel.component.salesforce.userName", "username");
            options.put("camel.component.salesforce.password", "password");
            options.put("camel.component.salesforce.clientId", "clientId");
        }

        // The Pub / Sub API uses gRPC, which WireMock cannot stub
        if (isMockingEnabled()) {
            try {
                pubSubServer = ServerBuilder.forPort(0)
                        .addService(new MockPubSubService())
                        .build()
                        .start();
            } catch (IOException e) {
                throw new RuntimeException("Failed to start the mock Pub / Sub API server", e);
            }
            options.put("camel.component.salesforce.pubSubHost", "localhost");
            options.put("camel.component.salesforce.pubSubPort", String.valueOf(pubSubServer.getPort()));
            // The mock Pub / Sub API server does not support TLS
            options.put("salesforce.pubsub.use-plain-text", "true");
        }
        return options;
    }

    @Override
    public void stop() {
        if (pubSubServer != null) {
            pubSubServer.shutdownNow();
        }
        super.stop();
    }
}
