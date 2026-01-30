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
package org.apache.camel.quarkus.component.openai.it;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.StubMappingTransformer;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.openai.core.ClientOptions;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;
import org.apache.camel.util.ObjectHelper;

public class OpenaiTestResource extends WireMockTestResourceLifecycleManager {
    private static final String OPENAI_API_URL = ClientOptions.PRODUCTION_URL;
    private static final String OPENAI_ENV_API_KEY = "OPENAI_API_KEY";
    private static final String OPENAI_ENV_BASE_URL = "OPENAI_BASE_URL";
    private static final String OPENAI_ENV_MODEL = "OPENAI_MODEL";

    @Override
    public Map<String, String> start() {
        Map<String, String> configuration = super.start();
        String wiremockUrl = configuration.get("wiremock.url");
        if (ObjectHelper.isNotEmpty(wiremockUrl)) {
            configuration.put("camel.component.openai.baseUrl", wiremockUrl);
        } else {
            configuration.put("camel.component.openai.baseUrl", OPENAI_API_URL);
        }

        configuration.put("camel.component.openai.model", envOrDefault(OPENAI_ENV_MODEL, "gpt-5"));
        configuration.put("camel.component.openai.apiKey", envOrDefault(OPENAI_ENV_API_KEY, "test-key"));
        return configuration;
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return envOrDefault(OPENAI_ENV_BASE_URL, OPENAI_API_URL);
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(OPENAI_ENV_API_KEY);
    }

    @Override
    protected void customizeWiremockConfiguration(WireMockConfiguration config) {
        // Removes openai-project header from saved mappings
        config.extensions(new StubMappingTransformer() {
            @Override
            public String getName() {
                return "camel-quarkus-openai-transformer";
            }

            @Override
            public StubMapping transform(StubMapping stubMapping, FileSource fileSource, Parameters parameters) {
                ResponseDefinition original = stubMapping.getResponse();
                HttpHeaders originalHeaders = original.getHeaders();
                List<HttpHeader> filteredHeaders = originalHeaders.all().stream()
                        .filter(h -> !h.keyEquals("openai-project"))
                        .collect(Collectors.toList());

                HttpHeaders newHeaders = new HttpHeaders(filteredHeaders);
                ResponseDefinition newResponse = ResponseDefinitionBuilder.like(original)
                        .withHeaders(newHeaders)
                        .build();

                stubMapping.setResponse(newResponse);
                return stubMapping;
            }
        });
    }
}
