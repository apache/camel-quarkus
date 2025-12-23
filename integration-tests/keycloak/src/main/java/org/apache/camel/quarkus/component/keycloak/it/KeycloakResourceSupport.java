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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.keycloak.KeycloakConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Base class for Keycloak resource classes providing shared dependencies and helper methods.
 */
public abstract class KeycloakResourceSupport {

    protected static final Logger LOG = Logger.getLogger(KeycloakResourceSupport.class);
    protected static final String COMPONENT_KEYCLOAK = "keycloak";

    @Inject
    protected CamelContext context;

    @Inject
    protected ProducerTemplate producerTemplate;

    @ConfigProperty(name = "keycloak.url")
    protected String keycloakUrl;

    @ConfigProperty(name = "keycloak.username")
    protected String keycloakUsername;

    @ConfigProperty(name = "keycloak.password")
    protected String keycloakPassword;

    @ConfigProperty(name = "keycloak.realm")
    protected String keycloakRealm;

    /**
     * Constructs the Keycloak endpoint URL with authentication parameters.
     *
     * @return The configured Keycloak endpoint URL
     */
    protected String getKeycloakEndpoint() {
        return String.format("keycloak:admin?serverUrl=%s&realm=%s&username=%s&password=%s",
                keycloakUrl, keycloakRealm, keycloakUsername, keycloakPassword);
    }

    /**
     * Helper method to get user ID by username.
     *
     * @param  realmName        The realm name
     * @param  username         The username
     * @return                  The user ID
     * @throws RuntimeException if the user is not found
     */
    protected String getUserIdByUsername(String realmName, String username) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        @SuppressWarnings("unchecked")
        List<UserRepresentation> users = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listUsers",
                null,
                headers,
                List.class);

        return users.stream()
                .filter(u -> username.equals(u.getUsername()))
                .map(UserRepresentation::getId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    /**
     * Helper method to get group ID by name.
     *
     * @param  realmName        The realm name
     * @param  groupName        The group name
     * @return                  The group ID
     * @throws RuntimeException if the group is not found
     */
    protected String getGroupIdByName(String realmName, String groupName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        @SuppressWarnings("unchecked")
        List<GroupRepresentation> groups = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listGroups",
                null,
                headers,
                List.class);

        return groups.stream()
                .filter(g -> groupName.equals(g.getName()))
                .map(GroupRepresentation::getId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupName));
    }

    /**
     * Helper method to get client UUID by clientId.
     *
     * @param  realmName        The realm name
     * @param  clientId         The client ID
     * @return                  The client UUID
     * @throws RuntimeException if the client is not found
     */
    protected String getClientUuidByClientId(String realmName, String clientId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        @SuppressWarnings("unchecked")
        List<ClientRepresentation> clients = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listClients",
                null,
                headers,
                List.class);

        return clients.stream()
                .filter(c -> clientId.equals(c.getClientId()))
                .map(ClientRepresentation::getId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));
    }

    /**
     * Helper method to get client scope ID by name.
     *
     * @param  realmName        The realm name
     * @param  scopeName        The scope name
     * @return                  The client scope ID
     * @throws RuntimeException if the scope is not found
     */
    protected String getClientScopeIdByName(String realmName, String scopeName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        @SuppressWarnings("unchecked")
        List<ClientScopeRepresentation> scopes = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listClientScopes",
                null,
                headers,
                List.class);

        return scopes.stream()
                .filter(s -> scopeName.equals(s.getName()))
                .map(ClientScopeRepresentation::getId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Client scope not found: " + scopeName));
    }
}
