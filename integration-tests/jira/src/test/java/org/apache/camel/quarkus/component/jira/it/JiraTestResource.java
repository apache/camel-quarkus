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
package org.apache.camel.quarkus.component.jira.it;

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class JiraTestResource extends WireMockTestResourceLifecycleManager {

    private static final String JIRA_ENV_URL = "JIRA_URL";
    private static final String JIRA_ENV_USERNAME = "JIRA_USERNAME";
    private static final String JIRA_ENV_PASSWORD = "JIRA_PASSWORD";
    private static final String JIRA_ENV_ISSUES_PROJECT_KEY = "JIRA_ISSUES_PROJECT_KEY";
    private static final String JIRA_ENV_OAUTH_ACCESS_TOKEN = "JIRA_OAUTH_ACCESS_TOKEN";
    private static final String JIRA_ENV_OAUTH_CONSUMER_KEY = "JIRA_OAUTH_CONSUMER_KEY";
    private static final String JIRA_ENV_OAUTH_PRIVATE_KEY = "JIRA_OAUTH_PRIVATE_KEY";
    private static final String JIRA_ENV_OAUTH_VERIFICATION_CODE = "JIRA_OAUTH_VERIFICATION_CODE";

    @Override
    public Map<String, String> start() {
        Map<String, String> options = super.start();
        String jiraUrl = envOrDefault(JIRA_ENV_URL, "http://localhost:8080");
        if (options.containsKey("wiremock.url")) {
            jiraUrl = options.get("wiremock.url");
        }

        options.put("jira.issues.project-key", envOrDefault(JIRA_ENV_ISSUES_PROJECT_KEY, "TEST"));
        options.put("camel.component.jira.jira-url", jiraUrl);

        if (isOauth()) {
            options.put("camel.component.jira.access-token", envOrDefault(JIRA_ENV_OAUTH_ACCESS_TOKEN, ""));
            options.put("camel.component.jira.consumer-key", envOrDefault(JIRA_ENV_OAUTH_CONSUMER_KEY, ""));
            options.put("camel.component.jira.private-key", envOrDefault(JIRA_ENV_OAUTH_PRIVATE_KEY, ""));
            options.put("camel.component.jira.verification-code", envOrDefault(JIRA_ENV_OAUTH_VERIFICATION_CODE, ""));
        } else {
            options.put("camel.component.jira.username", envOrDefault(JIRA_ENV_USERNAME, "jiratester"));
            options.put("camel.component.jira.password", envOrDefault(JIRA_ENV_PASSWORD, "tester123"));
        }

        return options;
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return envOrDefault(JIRA_ENV_URL, null);
    }

    @Override
    protected boolean isMockingEnabled() {
        if (isOauth()) {
            return !envVarsPresent(
                    JIRA_ENV_URL,
                    JIRA_ENV_ISSUES_PROJECT_KEY,
                    JIRA_ENV_OAUTH_ACCESS_TOKEN,
                    JIRA_ENV_OAUTH_CONSUMER_KEY,
                    JIRA_ENV_OAUTH_PRIVATE_KEY,
                    JIRA_ENV_OAUTH_VERIFICATION_CODE);
        }
        return !envVarsPresent(
                JIRA_ENV_URL,
                JIRA_ENV_USERNAME,
                JIRA_ENV_PASSWORD,
                JIRA_ENV_ISSUES_PROJECT_KEY);
    }

    private boolean isOauth() {
        return envVarsPresent(
                JIRA_ENV_OAUTH_ACCESS_TOKEN,
                JIRA_ENV_OAUTH_CONSUMER_KEY,
                JIRA_ENV_OAUTH_PRIVATE_KEY,
                JIRA_ENV_OAUTH_VERIFICATION_CODE);
    }
}
