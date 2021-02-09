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
package org.apache.camel.quarkus.component.pubnub.it;

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public class PubnubTestResource extends WireMockTestResourceLifecycleManager {

    private static final String PUBNUB_BASE_URL = "https://ps.pndsn.com";
    private static final String PUBNUB_ENV_PUBLISH_KEY = "PUBNUB_PUBLISH_KEY";
    private static final String PUBNUB_ENV_SUBSCRIBE_KEY = "PUBNUB_SUBSCRIBE_KEY";
    private static final String PUBNUB_ENV_SECRET_KEY = "PUBNUB_SECRET_KEY";

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();
        String wireMockUrl = properties.get("wiremock.url");
        if (wireMockUrl != null) {
            properties.put("pubnub.url", wireMockUrl.replace("http://", "") + "/");

            // This is purposely stubbed here before the PubNub consumer starts up
            server.stubFor(get(urlPathMatching("/v2/subscribe/(.*)/(.*)/0"))
                    .willReturn(aResponse().withBody("{\"t\":{\"t\":\"16127970351113041\",\"r\":12},\"m\":[]}")));
        }

        return CollectionHelper.mergeMaps(properties, CollectionHelper.mapOf(
                "pubnub.publish.key", envOrDefault(PUBNUB_ENV_PUBLISH_KEY, "test-publish-key"),
                "pubnub.subscribe.key", envOrDefault(PUBNUB_ENV_SUBSCRIBE_KEY, "test-subscribe-key"),
                "pubnub.secret.key", envOrDefault(PUBNUB_ENV_SECRET_KEY, "test-secret-key")));
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return PUBNUB_BASE_URL;
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(
                PUBNUB_ENV_PUBLISH_KEY,
                PUBNUB_ENV_SUBSCRIBE_KEY,
                PUBNUB_ENV_SECRET_KEY);
    }
}
