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
package org.apache.camel.quarkus.component.servicenow.it;

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;

public class ServicenowTestResource extends WireMockTestResourceLifecycleManager {

    private static final String SERVICENOW_ENV_INSTANCE = "SERVICENOW_INSTANCE";
    private static final String SERVICENOW_ENV_USERNAME = "SERVICENOW_USERNAME";
    private static final String SERVICENOW_ENV_PASSWORD = "SERVICENOW_PASSWORD";

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();
        String wiremockUrl = properties.get("wiremock.url");
        String apiUrl = wiremockUrl != null ? wiremockUrl : getRecordTargetBaseUrl();
        return CollectionHelper.mergeMaps(properties, CollectionHelper.mapOf(
                "camel.component.servicenow.instance-name", envOrDefault(SERVICENOW_ENV_INSTANCE, "fake"),
                "camel.component.servicenow.username", envOrDefault(SERVICENOW_ENV_USERNAME, "test"),
                "camel.component.servicenow.password", envOrDefault(SERVICENOW_ENV_PASSWORD, "2se3r3t"),
                "camel.component.servicenow.api-url", apiUrl));
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return String.format("https://%s.service-now.com/api", System.getenv(SERVICENOW_ENV_INSTANCE));
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(
                SERVICENOW_ENV_INSTANCE,
                SERVICENOW_ENV_USERNAME,
                SERVICENOW_ENV_PASSWORD);
    }
}
