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

import java.util.Map;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class SalesforceTestResource extends WireMockTestResourceLifecycleManager {
    private static final String SALESFORCE_BASE_URL = "login.salesforce.com";
    private static final String SALESFORCE_CLIENT_ID = "SALESFORCE_CLIENTID";
    private static final String SALESFORCE_CLIENT_SECRET = "SALESFORCE_CLIENTSECRET";
    private static final String SALESFORCE_USERNAME = "SALESFORCE_USERNAME";
    private static final String SALESFORCE_PASSWORD = "SALESFORCE_PASSWORD";

    @Override
    protected String getRecordTargetBaseUrl() {
        return SALESFORCE_BASE_URL;
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(SALESFORCE_CLIENT_ID, SALESFORCE_CLIENT_SECRET, SALESFORCE_USERNAME, SALESFORCE_PASSWORD);
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
        return options;
    }

    @Override
    protected void customizeWiremockConfiguration(WireMockConfiguration config) {
        config.extensions(new ResponseTemplateTransformer(false));
    }

}
