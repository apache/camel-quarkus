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
package org.apache.camel.quarkus.component.telegram.it;

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;

public class TelegramTestResource extends WireMockTestResourceLifecycleManager {

    private static final String TELEGRAM_API_BASE_URL = "https://api.telegram.org";
    private static final String TELEGRAM_ENV_AUTHORIZATION_TOKEN = "TELEGRAM_AUTHORIZATION_TOKEN";
    private static final String TELEGRAM_ENV_CHAT_ID = "TELEGRAM_CHAT_ID";
    private static final String TELEGRAM_ENV_WEBHOOK_EXTERNAL_URL = "TELEGRAM_WEBHOOK_EXTERNAL_URL";
    private static final String TELEGRAM_ENV_WEBHOOK_AUTHORIZATION_TOKEN = "TELEGRAM_WEBHOOK_AUTHORIZATION_TOKEN";

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();
        String wireMockUrl = properties.get("wiremock.url");
        String baseUri = wireMockUrl != null ? wireMockUrl : TELEGRAM_API_BASE_URL;
        return CollectionHelper.mergeMaps(properties,
                CollectionHelper.mapOf("camel.component.telegram.base-uri", baseUri));
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return TELEGRAM_API_BASE_URL;
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(TELEGRAM_ENV_AUTHORIZATION_TOKEN, TELEGRAM_ENV_CHAT_ID, TELEGRAM_ENV_WEBHOOK_EXTERNAL_URL,
                TELEGRAM_ENV_WEBHOOK_AUTHORIZATION_TOKEN);
    }
}
