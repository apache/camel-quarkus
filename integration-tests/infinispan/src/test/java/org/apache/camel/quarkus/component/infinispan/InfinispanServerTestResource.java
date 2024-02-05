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
package org.apache.camel.quarkus.component.infinispan;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.DockerClientFactory;

public class InfinispanServerTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Map<String, String> config = new HashMap<>();
        String host = String.format("%s:31222", DockerClientFactory.instance().dockerHostIpAddress());
        config.put("camel.component.infinispan.autowired-enabled", "false");
        config.put("camel.component.infinispan.hosts", host);
        config.put("camel.component.infinispan.username", "admin");
        config.put("camel.component.infinispan.password", "password");
        config.put("camel.component.infinispan.secure", "true");
        config.put("camel.component.infinispan.security-realm", "default");
        config.put("camel.component.infinispan.sasl-mechanism", "DIGEST-MD5");
        config.put("camel.component.infinispan.security-server-name", "infinispan");
        config.put("camel.component.infinispan.configuration-properties", "#additionalConfig");

        return config;
    }

    @Override
    public void stop() {

    }
}
