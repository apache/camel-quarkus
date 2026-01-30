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
package org.apache.camel.quarkus.component.keycloak.it;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.keycloak.security.KeycloakSecurityPolicy;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class KeycloakRouteBuilder extends RouteBuilder {
    @Override
    public void configure() {
        // Values provided by KeycloakTestResource
        var config = ConfigProvider.getConfig();

        String serverUrl = config.getValue("keycloak.url", String.class);
        String realm = config.getValue("test.realm", String.class);
        String clientId = config.getValue("test.client.id", String.class);
        String clientSecret = config.getValue("test.client.secret", String.class);

        // Route 1: secure default
        KeycloakSecurityPolicy secureDefaultPolicy = new KeycloakSecurityPolicy();
        secureDefaultPolicy.setServerUrl(serverUrl);
        secureDefaultPolicy.setRealm(realm);
        secureDefaultPolicy.setClientId(clientId);
        secureDefaultPolicy.setClientSecret(clientSecret);
        secureDefaultPolicy.setValidateTokenBinding(true);
        secureDefaultPolicy.setAllowTokenFromHeader(true);
        secureDefaultPolicy.setPreferPropertyOverHeader(true);

        from("direct:secure-default")
                .routeId("secure-default")
                .autoStartup(true)
                .policy(secureDefaultPolicy)
                .transform().constant("Access granted - secure default");

        // Route 2: maximum security - headers disabled
        KeycloakSecurityPolicy maxSecurityPolicy = new KeycloakSecurityPolicy();
        maxSecurityPolicy.setServerUrl(serverUrl);
        maxSecurityPolicy.setRealm(realm);
        maxSecurityPolicy.setClientId(clientId);
        maxSecurityPolicy.setClientSecret(clientSecret);
        maxSecurityPolicy.setAllowTokenFromHeader(false);

        from("direct:max-security")
                .routeId("max-security")
                .autoStartup(true)
                .policy(maxSecurityPolicy)
                .transform().constant("Access granted - max security");

        // Route 3: legacy unsafe - prefer property disabled
        KeycloakSecurityPolicy legacyPolicy = new KeycloakSecurityPolicy();
        legacyPolicy.setServerUrl(serverUrl);
        legacyPolicy.setRealm(realm);
        legacyPolicy.setClientId(clientId);
        legacyPolicy.setClientSecret(clientSecret);
        legacyPolicy.setValidateTokenBinding(true);
        legacyPolicy.setAllowTokenFromHeader(true);
        legacyPolicy.setPreferPropertyOverHeader(false);

        from("direct:legacy-unsafe")
                .routeId("legacy-unsafe")
                .autoStartup(true)
                .policy(legacyPolicy)
                .transform().constant("SHOULD NOT REACH HERE");

        // Route 4: Admin-only route
        KeycloakSecurityPolicy adminPolicy = new KeycloakSecurityPolicy();
        adminPolicy.setServerUrl(serverUrl);
        adminPolicy.setRealm(realm);
        adminPolicy.setClientId(clientId);
        adminPolicy.setClientSecret(clientSecret);
        adminPolicy.setRequiredRoles(List.of("admin"));
        adminPolicy.setPreferPropertyOverHeader(true);

        from("direct:admin-only")
                .routeId("admin-only")
                .autoStartup(true)
                .policy(adminPolicy)
                .transform().constant("Admin access granted");
    }
}
