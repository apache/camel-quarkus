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

import java.util.Map;

import org.apache.camel.quarkus.component.infinispan.common.InfinispanCommonServerTestResource;
import org.infinispan.commons.util.OS;

public class InfinispanServerTestResource extends InfinispanCommonServerTestResource {

    @Override
    public Map<String, String> start() {
        Map<String, String> config = super.start();
        config.put("quarkus.infinispan-client.server-list", getServerList());
        config.put("quarkus.infinispan-client.auth-username", USER);
        config.put("quarkus.infinispan-client.auth-password", PASS);
        config.put("quarkus.infinispan-client.auth-realm", "default");
        config.put("quarkus.infinispan-client.sasl-mechanism", "DIGEST-MD5");
        config.put("quarkus.infinispan-client.auth-server-name", "infinispan");

        if (OS.getCurrentOs().equals(OS.MAC_OS) || OS.getCurrentOs().equals(OS.WINDOWS)) {
            config.put("quarkus.infinispan-client.client-intelligence", "BASIC");
        }

        return config;
    }
}
