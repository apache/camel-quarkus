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
package org.apache.camel.quarkus.component.weather.it;

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;

public class WeatherTestResource extends WireMockTestResourceLifecycleManager {

    private static final String OPEN_WEATHER_API_BASE_URL = "https://api.openweathermap.org";
    private static final String WEATHER_API_ID_ENV = "WEATHER_API_ID";

    @Override
    public Map<String, String> start() {
        return CollectionHelper.mergeMaps(super.start(), CollectionHelper.mapOf(
                "weather.api.id", envOrDefault(WEATHER_API_ID_ENV, "test_api_id")));
    }

    @Override
    public String getRecordTargetBaseUrl() {
        return OPEN_WEATHER_API_BASE_URL;
    }

    @Override
    public boolean isMockingEnabled() {
        return !envVarsPresent(WEATHER_API_ID_ENV);
    }
}
