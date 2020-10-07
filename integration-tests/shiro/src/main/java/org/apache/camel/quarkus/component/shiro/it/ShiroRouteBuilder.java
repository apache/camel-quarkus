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
package org.apache.camel.quarkus.component.shiro.it;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.camel.CamelAuthorizationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.shiro.security.ShiroSecurityPolicy;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

public class ShiroRouteBuilder extends RouteBuilder {
    public static final String DIRECT_SECURE_ENDPOINT = "direct:secureEndpoint";
    public static final String DIRECT_SECURE_WITH_ROLES = "direct:secureWithRoles";
    public static final String DIRECT_SECURE_WITH_PERMISSIONS = "direct:secureWithPermissions";

    @Override
    public void configure() throws Exception {
        String securityConfig = "classpath:config/securityConfig.ini";

        onException(CamelAuthorizationException.class, UnknownAccountException.class, IncorrectCredentialsException.class,
                LockedAccountException.class, AuthenticationException.class).to("mock:authenticationException");
        //policy ignores roles or permissions
        final ShiroSecurityPolicy securityPolicy = new ShiroSecurityPolicy(securityConfig, ShiroResource.passPhrase);
        securityPolicy.setBase64(true);

        from(DIRECT_SECURE_ENDPOINT).policy(securityPolicy).to("mock:success");

        //policy respects roles
        List<String> rolesList = new ArrayList<>();
        rolesList.add("sec-level2");
        rolesList.add("sec-level3");

        final ShiroSecurityPolicy securityPolicyWithRoles = new ShiroSecurityPolicy(securityConfig,
                ShiroResource.passPhrase,
                true);
        securityPolicyWithRoles.setRolesList(rolesList);

        from(DIRECT_SECURE_WITH_ROLES).policy(securityPolicyWithRoles).to("mock:success");

        //policy respects permissions
        List<Permission> permissionsList = Collections.singletonList(new WildcardPermission("earth1:writeonly:*"));

        final ShiroSecurityPolicy securityPolicyWithPermissions = new ShiroSecurityPolicy(securityConfig,
                ShiroResource.passPhrase, true, permissionsList);

        from(DIRECT_SECURE_WITH_PERMISSIONS).policy(securityPolicyWithPermissions).to("mock:success");

    }
}
