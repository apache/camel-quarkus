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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.shiro.security.ShiroSecurityToken;

@QuarkusTest
class ShiroTest {

    enum AUTHORIZATION {
        none(ShiroRouteBuilder.DIRECT_SECURE_ENDPOINT),
        roles(ShiroRouteBuilder.DIRECT_SECURE_WITH_ROLES),
        permissions(ShiroRouteBuilder.DIRECT_SECURE_WITH_PERMISSIONS);

        private String path;

        AUTHORIZATION(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    static ShiroSecurityToken SHELDON_TOKEN = new ShiroSecurityToken("sheldon", "earth2");
    private static ShiroSecurityToken IRMA_TOKEN = new ShiroSecurityToken("irma", "password");
    private static ShiroSecurityToken FRED_TOKEN = new ShiroSecurityToken("fred", "fred");
    private static ShiroSecurityToken SEC_LEVEL1 = SHELDON_TOKEN;
    private static ShiroSecurityToken SEC_LEVEL2 = IRMA_TOKEN;
    private static ShiroSecurityToken SEC_LEVEL3 = FRED_TOKEN;
    private static ShiroSecurityToken WRONG_TOKEN = new ShiroSecurityToken("sheldon", "wrong");

    //@Test
    public void testHeaders() {
        test("headers", SHELDON_TOKEN, AUTHORIZATION.none, true);
        test("headers", WRONG_TOKEN, AUTHORIZATION.none, false);
    }

    //@Test
    public void testToken() {
        test("token", IRMA_TOKEN, AUTHORIZATION.none, true);
        test("token", WRONG_TOKEN, AUTHORIZATION.none, false);
    }

    //@Test
    public void testBase64() {
        test("base64", FRED_TOKEN, AUTHORIZATION.none, true);
        test("base64", WRONG_TOKEN, AUTHORIZATION.none, false);
    }

    //@Test
    public void testTokenWithRoles() {
        test("headers", SEC_LEVEL1, AUTHORIZATION.roles, false);
        test("token", SEC_LEVEL2, AUTHORIZATION.roles, true);
        test("token", SEC_LEVEL3, AUTHORIZATION.roles, true);
    }

    //@Test
    public void testTokenWithPermissions() {
        test("token", SEC_LEVEL1, AUTHORIZATION.permissions, false);
        test("headers", SEC_LEVEL2, AUTHORIZATION.permissions, true);
        test("headers", SEC_LEVEL3, AUTHORIZATION.permissions, true);
    }

    void test(String path, ShiroSecurityToken token, AUTHORIZATION authorization, boolean expectSuccess) {

        RestAssured.given()
                .queryParam("expectSuccess", expectSuccess)
                .queryParam("path", authorization.getPath())
                .contentType(ContentType.JSON)
                .body(token)
                .post("/shiro/" + path)
                .then()
                .statusCode(204);
    }

}
