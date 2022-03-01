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
package org.apache.camel.quarkus.component.slack.it;

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.ConfigProvider;

public class SlackTestResource extends WireMockTestResourceLifecycleManager {

    private static final String SLACK_API_BASE_URL = "https://slack.com";
    private static final String SLACK_ENV_WEBHOOK_URL = "SLACK_WEBHOOK_URL";
    private static final String SLACK_ENV_SERVER_URL = "SLACK_SERVER_URL";
    private static final String SLACK_ENV_TOKEN = "SLACK_TOKEN";

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();
        String wiremockUrl = properties.get("wiremock.url");
        String serverUrl = wiremockUrl != null ? wiremockUrl
                : ConfigProvider.getConfig().getValue(SLACK_ENV_SERVER_URL, String.class);
        String webhookUrl = wiremockUrl != null ? wiremockUrl + "/services/webhook"
                : ConfigProvider.getConfig().getValue(SLACK_ENV_WEBHOOK_URL, String.class);
        return CollectionHelper.mergeMaps(properties, CollectionHelper.mapOf(
                "slack.webhook.url", webhookUrl,
                "slack.server-url", serverUrl,
                "slack.token", envOrDefault(SLACK_ENV_TOKEN, "test-token")));
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return SLACK_API_BASE_URL;
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(
                SLACK_ENV_WEBHOOK_URL,
                SLACK_ENV_SERVER_URL,
                SLACK_ENV_TOKEN);
    }
}
