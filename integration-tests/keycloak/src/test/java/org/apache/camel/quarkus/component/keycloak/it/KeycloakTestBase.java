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

import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for Keycloak integration tests.
 * Provides common test data and RestAssured configuration.
 */
@QuarkusTestResource(KeycloakTestResource.class)
public abstract class KeycloakTestBase {

    // Test data - use unique names to avoid conflicts
    protected static final String TEST_REALM_NAME = "test-realm-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_USER_NAME = "test-user-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_USER_PASSWORD = "Test@password123";
    protected static final String TEST_ROLE_NAME = "test-role-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_GROUP_NAME = "test-group-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_CLIENT_ID = "test-client-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_CLIENT_SECRET = "test-client-secret";
    protected static final String TEST_CLIENT_ROLE_NAME = "test-client-role-"
            + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_CLIENT_SCOPE_NAME = "test-scope-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_IDP_ALIAS = "test-idp-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_AUTHZ_CLIENT_ID = "test-authz-client-"
            + UUID.randomUUID().toString().substring(0, 8);
    protected static String TEST_RESOURCE_ID; // Set after creation
    protected static String TEST_POLICY_ID; // Set after creation
    protected static String TEST_PERMISSION_ID; // Set after creation

    @BeforeAll
    public static void configureRestAssured() {
        // Configure REST-assured to ignore unknown properties when deserializing
        // This is needed because the Keycloak server may return newer fields
        // that the client representation classes don't know about
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory(
                        (cls, charset) -> {
                            ObjectMapper mapper = new ObjectMapper();
                            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                            return mapper;
                        }));
    }

    protected String config(String name) {
        return ConfigProvider.getConfig().getValue(name, String.class);
    }
}
