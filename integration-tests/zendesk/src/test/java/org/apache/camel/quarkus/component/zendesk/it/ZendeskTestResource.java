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
package org.apache.camel.quarkus.component.zendesk.it;

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;

public class ZendeskTestResource extends WireMockTestResourceLifecycleManager {

    private static final String ZENDESK_ENV_USERNAME = "ZENDESK_USERNAME";
    private static final String ZENDESK_ENV_PASSWORD = "ZENDESK_PASSWORD";
    private static final String ZENDESK_ENV_SERVER_URL = "ZENDESK_SERVER_URL";

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();
        String wiremockUrl = properties.get("wiremock.url");
        String serverUrl = wiremockUrl != null ? wiremockUrl : System.getenv(ZENDESK_ENV_SERVER_URL);
        return CollectionHelper.mergeMaps(properties, CollectionHelper.mapOf(
                "camel.component.zendesk.username", envOrDefault(ZENDESK_ENV_USERNAME, "test"),
                "camel.component.zendesk.password", envOrDefault(ZENDESK_ENV_PASSWORD, "2se3r3t"),
                "camel.component.zendesk.server-url", serverUrl));
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return System.getenv(ZENDESK_ENV_SERVER_URL);
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(
                ZENDESK_ENV_USERNAME,
                ZENDESK_ENV_PASSWORD,
                ZENDESK_ENV_SERVER_URL);
    }
}
