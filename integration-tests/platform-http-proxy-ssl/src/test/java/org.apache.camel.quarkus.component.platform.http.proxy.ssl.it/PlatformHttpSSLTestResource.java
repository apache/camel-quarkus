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
package org.apache.camel.quarkus.component.platform.http.proxy.ssl.it;

import java.util.Map;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class PlatformHttpSSLTestResource extends WireMockTestResourceLifecycleManager {
    private static final String PLATFORM_ORIGIN_HOST = "PLATFORM_ORIGIN_HOST";
    private static final String PLATFORM_ORIGIN_PORT = "PLATFORM_ORIGIN_PORT";

    @Override
    protected String getRecordTargetBaseUrl() {
        return "/";
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(PLATFORM_ORIGIN_HOST, PLATFORM_ORIGIN_PORT);
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> options = super.start();
        if (options.containsKey("wiremock.url.ssl")) {
            String wiremockUrl = options.get("wiremock.url.ssl");
            options.put("platform.origin.url", wiremockUrl);
        }
        return options;
    }

    @Override
    protected void customizeWiremockConfiguration(WireMockConfiguration config) {
        // add an SSL port
        config.dynamicHttpsPort();
        // Either a path to a file or a resource on the classpath
        config.keystorePath("ssl/keystore.p12");
        // The password used to access the keystore. Defaults to "password" if omitted
        config.keystorePassword("changeit");
        // The password used to access individual keys in the keystore. Defaults to "password" if omitted
        config.keyManagerPassword("changeit");
    }
}
