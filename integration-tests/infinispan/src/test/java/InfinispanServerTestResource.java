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

import java.util.Map;

import org.apache.camel.quarkus.component.infinispan.common.InfinispanCommonServerTestResource;

public class InfinispanServerTestResource extends InfinispanCommonServerTestResource {

    @Override
    public Map<String, String> start() {
        Map<String, String> config = super.start();

        config.put("camel.component.infinispan.autowired-enabled", "false");
        config.put("camel.component.infinispan.hosts", getServerList());
        config.put("camel.component.infinispan.username", USER);
        config.put("camel.component.infinispan.password", PASS);
        config.put("camel.component.infinispan.secure", "true");
        config.put("camel.component.infinispan.security-realm", "default");
        config.put("camel.component.infinispan.sasl-mechanism", "DIGEST-MD5");
        config.put("camel.component.infinispan.security-server-name", "infinispan");
        config.put("camel.component.infinispan.configuration-properties", "#additionalConfig");

        return config;
    }
}
