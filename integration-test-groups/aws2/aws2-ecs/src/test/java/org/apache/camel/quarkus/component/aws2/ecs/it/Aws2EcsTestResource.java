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
package org.apache.camel.quarkus.component.aws2.ecs.it;

import java.util.Map;

import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class Aws2EcsTestResource extends WireMockTestResourceLifecycleManager {

    private static final String ECS_ACCESS_KEY = "AWS_ACCESS_KEY";
    private static final String ECS_SECRET_KEY = "AWS_SECRET_KEY";
    private static final String ECS_REGION = "AWS_REGION";

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();

        if (isMockingEnabled()) {
            String wiremockUrl = properties.get("wiremock.url");
            properties.put("camel.component.aws2-ecs.uri-endpoint-override", wiremockUrl);
            properties.put("camel.component.aws2-ecs.override-endpoint", "true");
            properties.put("camel.component.aws2-ecs.access-key", "test");
            properties.put("camel.component.aws2-ecs.secret-key", "test");
            properties.put("camel.component.aws2-ecs.region", "us-east-1");
        } else {
            properties.put("camel.component.aws2-ecs.access-key", envOrDefault(ECS_ACCESS_KEY, ""));
            properties.put("camel.component.aws2-ecs.secret-key", envOrDefault(ECS_SECRET_KEY, ""));
            properties.put("camel.component.aws2-ecs.region", envOrDefault(ECS_REGION, "us-east-1"));
        }

        return properties;
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return "https://ecs." + envOrDefault(ECS_REGION, "us-east-1") + ".amazonaws.com";
    }

    @Override
    protected boolean isMockingEnabled() {
        return MockBackendUtils.startMockBackend();
    }
}
