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
package org.apache.camel.quarkus.component.knative.producer.it;

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class KnativeTestResource extends WireMockTestResourceLifecycleManager {

    private static final String KNATIVE_CHANNEL_URL = "KNATIVE_CHANNEL_URL";
    private static final String KNATIVE_BROKER_URL = "KNATIVE_BROKER_URL";
    private static final String KNATIVE_SERVICE_URL = "KNATIVE_SERVICE_URL";

    @Override
    protected String getRecordTargetBaseUrl() {
        return "/";
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(KNATIVE_CHANNEL_URL, KNATIVE_BROKER_URL, KNATIVE_SERVICE_URL);
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> options = super.start();
        if (options.containsKey("wiremock.url")) {
            String wiremockUrl = options.get("wiremock.url");
            options.put("channel.test.url", String.format("%s/channel-test", wiremockUrl));
            options.put("broker.test.url", String.format("%s/broker-test", wiremockUrl));
            options.put("service.test.url", String.format("%s/service-test", wiremockUrl));
        }
        return options;
    }
}
