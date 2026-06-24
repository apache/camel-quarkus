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
package org.apache.camel.quarkus.component.aws.cloudtrail.it;

import java.util.Map;

import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class AwsCloudtrailTestResource extends WireMockTestResourceLifecycleManager {

    private static final String CLOUDTRAIL_REGION = "CLOUDTRAIL_REGION";
    private static final String AWS_ACCESS_KEY = "AWS_ACCESS_KEY";
    private static final String AWS_SECRET_KEY = "AWS_SECRET_KEY";

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();

        if (isMockingEnabled()) {
            String wiremockUrl = properties.get("wiremock.url");
            properties.put("quarkus.cloudtrail.endpoint-override", wiremockUrl);
            properties.put("aws.cloudtrail.region", envOrDefault(CLOUDTRAIL_REGION, "us-east-1"));
            properties.put("aws.cloudtrail.access-key", "test");
            properties.put("aws.cloudtrail.secret-key", "test");
        } else {
            properties.put("aws.cloudtrail.region", envOrDefault(CLOUDTRAIL_REGION, "us-east-1"));
            properties.put("aws.cloudtrail.access-key", envOrDefault(AWS_ACCESS_KEY, ""));
            properties.put("aws.cloudtrail.secret-key", envOrDefault(AWS_SECRET_KEY, ""));
        }

        return properties;
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return "https://cloudtrail." + envOrDefault(CLOUDTRAIL_REGION, "us-east-1") + ".amazonaws.com";
    }

    @Override
    protected boolean isMockingEnabled() {
        return MockBackendUtils.startMockBackend();
    }
}
