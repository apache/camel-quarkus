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
package org.apache.camel.quarkus.component.netty.http.auth;

import java.security.Principal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.apache.camel.component.netty.http.HttpPrincipal;
import org.apache.camel.component.netty.http.SecurityAuthenticator;

public class TestAuthenticator implements SecurityAuthenticator {
    private static final Map<String, String> AUTH = Map.of(
            "admin", "adminpass",
            "guest", "guestpass");
    private static final Map<String, String> ROLES = Map.of(
            "admin", "admin,guest",
            "guest", "guest");

    private String name;

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setRoleClassNames(String names) {
    }

    @Override
    public Subject login(HttpPrincipal principal) throws LoginException {
        if (!AUTH.containsKey(principal.getUsername()) || !AUTH.get(principal.getUsername()).equals(principal.getPassword())) {
            return null;
        }

        Subject subject = new Subject();
        subject.getPrincipals().addAll(Arrays.stream(ROLES.get(principal.getUsername()).split(",")).map(TestRolePrincipal::new)
                .collect(Collectors.toSet()));
        return subject;
    }

    @Override
    public void logout(Subject subject) throws LoginException {
    }

    @Override
    public String getUserRoles(Subject subject) {
        return subject.getPrincipals().stream().map(Principal::getName).collect(Collectors.joining(","));
    }
}
