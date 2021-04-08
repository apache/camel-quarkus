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
package org.apache.camel.quarkus.component.openstack.it;

import java.util.Map;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class OpenStackTestResource extends WireMockTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();
        String wiremockUrl = properties.get("wiremock.url");
        properties.put("camel.openstack.test.host-url", wiremockUrl);
        return properties;
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return null;
    }

    @Override
    protected boolean isMockingEnabled() {
        return true;
    }

    @Override
    protected void customizeWiremockConfiguration(WireMockConfiguration config) {
        config.extensions(new ResponseTemplateTransformer(false));
    }

}
