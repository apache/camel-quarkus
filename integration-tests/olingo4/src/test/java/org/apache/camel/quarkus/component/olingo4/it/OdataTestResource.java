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
package org.apache.camel.quarkus.component.olingo4.it;

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;
import org.apache.camel.util.ObjectHelper;

public class OdataTestResource extends WireMockTestResourceLifecycleManager {
    private static final String ODATA_API_BASE_URL = "https://services.odata.org/TripPinRESTierService";
    private static final String ENV_ODATA_API_BASE_URL = "ODATA_API_BASE_URL";

    @Override
    public Map<String, String> start() {
        Map<String, String> configuration = super.start();

        String wiremockUrl = configuration.get("wiremock.url");
        if (ObjectHelper.isNotEmpty(wiremockUrl)) {
            configuration.put("olingo4.test.url", wiremockUrl);
        } else if (!isMockingEnabled()) {
            configuration.put("olingo4.test.url", System.getenv(ENV_ODATA_API_BASE_URL));
        }

        return configuration;
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return ODATA_API_BASE_URL;
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(ENV_ODATA_API_BASE_URL);
    }

    @Override
    protected boolean isDeleteRecordedMappingsOnError() {
        // we are testing scenario where we delete record and then we want to verify it was deleted (which returns non success code)
        return false;
    }
}
