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
package org.apache.camel.quarkus.component.odata.it;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class OdataTestResource extends WireMockTestResourceLifecycleManager {
    private static final String ODATA_BASE_URL = "https://services.odata.org";
    private static final String ENV_ODATA_BASE_URL = "ODATA_BASE_URL";

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();
        String baseUrl = properties.getOrDefault("wiremock.url", System.getenv(ENV_ODATA_BASE_URL));
        properties.put("odata.service.url", createSession(baseUrl));
        return properties;
    }

    /**
     * The TripPin service keeps changes per session. It redirects to a new session URL like
     * /V4/(S(abc))/TripPinServiceRW/
     * There is no API to delete a session, the service expires it on its own.
     */
    private static String createSession(String baseUrl) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/V4/TripPinServiceRW/")).build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            String location = response.headers().firstValue("Location")
                    .orElseThrow(() -> new IllegalStateException("No TripPin session redirect received"));
            return baseUrl + location.substring(0, location.length() - 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void customizeWiremockConfiguration(WireMockConfiguration config) {
        // Jetty appends --gzip to the ETag of compressed responses
        config.gzipDisabled(true);
    }

    @Override
    protected void customizeRecordSpec(RecordSpecBuilder recordSpec) {
        // Stubs require the ETag sent as If-Match, like the real service
        recordSpec.captureHeader("If-Match");
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return ODATA_BASE_URL;
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(ENV_ODATA_BASE_URL);
    }

    @Override
    protected boolean isDeleteRecordedMappingsOnError() {
        // The session redirect and the stale ETag update are expected non 2xx responses
        return false;
    }
}
