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
package org.apache.camel.quarkus.component.xchange.it;

import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public abstract class XchangeTestResourceBase extends WireMockTestResourceLifecycleManager {
    private boolean mockingEnabled = true;

    XchangeTestResourceBase() {
        if (!MockBackendUtils.startMockBackend(false)) {
            LOG.infof("Checking the status of the %s API", getCryptoExchangeName());
            if (isCryptoApiAccessible()) {
                LOG.infof("Real backend will be used for %s", getCryptoExchangeName());
                this.mockingEnabled = false;
            } else {
                LOG.warnf("Falling back to mock backend for %s as %s is not accessible", getCryptoExchangeName(),
                        getRecordTargetBaseUrl());
            }
        }
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> options = super.start();
        String wireMockUrl = options.get("wiremock.url");
        if (wireMockUrl != null) {
            options.put(getWireMockUrlProperty(), wireMockUrl);
        }
        return options;
    }

    @Override
    protected boolean isMockingEnabled() {
        return mockingEnabled;
    }

    String getWireMockUrlProperty() {
        return String.format("wiremock.%s.url", getCryptoExchangeName());
    }

    boolean isCryptoApiAccessible() {
        try {
            JsonPath body = RestAssured.get(getHealthEndpointUrl())
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .jsonPath();
            String status = body.getString(getHealthStatusField());
            LOG.infof("Status of the %s API is %s", getCryptoExchangeName(), status);
            return status != null && status.equals(getExpectedHealthStatus());
        } catch (Exception e) {
            LOG.warnf(e, "Failed to contact the %s API", getCryptoExchangeName());
            return false;
        }
    }

    /**
     * Gets the name of the crypto exchange.
     *
     * @return The name of the crypto exchange
     */
    abstract String getCryptoExchangeName();

    /**
     * Gets the URL of the health endpoint for the crypto exchange API.
     *
     * @return The URL of the crypto exchange API health endpoint
     */
    abstract String getHealthEndpointUrl();

    /**
     * Gets the name of the JSON field containing the crypto exchange health status.
     *
     * @return The name of the JSON field containing the crypto exchange health status
     */
    abstract String getHealthStatusField();

    /**
     * Gets the expected value of the JSON field containing the crypto exchange health status.
     *
     * @return The expected value of the JSON field containing the crypto exchange health status
     */
    abstract String getExpectedHealthStatus();
}
