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
package org.apache.camel.quarkus.component.milo.it;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;

import static org.apache.camel.quarkus.component.milo.it.MiloRoutes.SECURE_SERVER_ITEM_ID;
import static org.apache.camel.quarkus.component.milo.it.MiloRoutes.SERVER_CREDENTIALS;

public class MiloTestResource implements QuarkusTestResourceLifecycleManager {
    @Override
    public Map<String, String> start() {
        Map<String, String> configuration = new HashMap<>();
        Stream.of("milo-server", "milo-" + SECURE_SERVER_ITEM_ID).forEach(miloServerComponentName -> {
            String prefix = "camel.component.%s.".formatted(miloServerComponentName);
            configuration.put(prefix + "port", Integer.toString(AvailablePortFinder.getNextAvailable()));
            configuration.put(prefix + "userAuthenticationCredentials", SERVER_CREDENTIALS);
            configuration.put(prefix + "usernameSecurityPolicyUri", "None");
            configuration.put(prefix + "securityPoliciesById", "None");
            configuration.put(prefix + "enableAnonymousAuthentication", "true");
        });
        return configuration;
    }

    @Override
    public void stop() {
        AvailablePortFinder.releaseReservedPorts();
    }
}
