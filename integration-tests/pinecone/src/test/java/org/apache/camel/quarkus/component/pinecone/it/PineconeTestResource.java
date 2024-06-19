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
package org.apache.camel.quarkus.component.pinecone.it;

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;

public class PineconeTestResource extends WireMockTestResourceLifecycleManager {
    public static final String PINECONE_API_BASE_URL = "https://api.pinecone.io";
    private static final String PINECONE_API_KEY_ENV = "PINECONE_API_KEY";

    @Override
    public Map<String, String> start() {
        return CollectionHelper.mergeMaps(super.start(), CollectionHelper.mapOf(
                "camel.component.pinecone.token", envOrDefault(PINECONE_API_KEY_ENV, "test-pinecone-api-key")));
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return PINECONE_API_BASE_URL;
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent("PINECONE_API_KEY");
    }
}
