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
package org.apache.camel.quarkus.component.digitalocean.it;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class DigitaloceanTestResource extends WireMockTestResourceLifecycleManager {
    private static final String DIGITALOCEAN_BASE_URL = "api.digitalocean.com";
    private static final String DIGITALOCEAN_AUTH_TOKEN = "DIGITALOCEAN_AUTH_TOKEN";
    private static final String DIGITALOCEAN_PUBLIC_KEY = "DIGITALOCEAN_PUBLIC_KEY";

    @Override
    protected String getRecordTargetBaseUrl() {
        return DIGITALOCEAN_BASE_URL;
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(DIGITALOCEAN_AUTH_TOKEN, DIGITALOCEAN_PUBLIC_KEY);
    }
}
