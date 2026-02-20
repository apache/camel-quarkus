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

import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(KeycloakTestResource.class)
public class KeycloakSecurityPolicyTestBase extends KeycloakTestBase {
    // Test users
    protected static final String ADMIN_USER = "admin-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String ADMIN_PASSWORD = "admin123";
    protected static final String NORMAL_USER = "user-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String NORMAL_PASSWORD = "user123";
    protected static final String ATTACKER_USER = "attacker-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String ATTACKER_PASSWORD = "attacker123";

    // Test roles
    protected static final String ADMIN_ROLE = "admin";
    protected static final String USER_ROLE = "user";
}
