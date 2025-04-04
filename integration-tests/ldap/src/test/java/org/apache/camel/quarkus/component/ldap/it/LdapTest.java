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
package org.apache.camel.quarkus.component.ldap.it;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestCertificates(certificates = {
        @Certificate(name = "ldap", formats = {
                Format.PKCS12 }, password = "changeit") })
@QuarkusTest
@QuarkusTestResource(LdapTestResource.class)
class LdapTest {

    /**
     * Calls a Camel route to search for LDAP entries where the uid is "tcruise".
     * The test is run in both SSL and non-SSL modes.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @ValueSource(strings = { "http", "ssl", "originalConfig", "additionalOptions" })
    public void ldapSearchTest(String direct) throws Exception {
        TypeRef<List<Map<String, Object>>> typeRef = new TypeRef<>() {
        };
        List<Map<String, Object>> results = RestAssured.given()
                .queryParam("ldapQuery", "tcruise")
                .get("/ldap/search/" + direct)
                .then()
                .statusCode(200)
                .extract().as(typeRef);

        assertEquals(1, results.size());
        assertEquals("Tom Cruise", results.get(0).get("cn"));
    }

    /**
     * Tests the escaping of search values using the
     * {@link org.apache.camel.component.ldap.LdapHelper} class.
     *
     * @throws Exception
     */
    @Test
    public void ldapHelperTest() throws Exception {
        TypeRef<List<Map<String, Object>>> typeRef = new TypeRef<>() {
        };

        // Verfiy that calling the unsafe endpoint with a wildcard returns multiple results.
        List<Map<String, Object>> results = RestAssured.given()
                .queryParam("ldapQuery", "test*")
                .get("/ldap/search/http")
                .then()
                .statusCode(200)
                .extract().as(typeRef);
        assertEquals(3, results.size());
        assertEquals(List.of("test1", "test2", "testNoOU"),
                results.stream().map(r -> r.get("uid")).collect(Collectors.toList()));

        // Verify that the same query passed to the safeSearch returns no matching results.
        results = RestAssured.given()
                .queryParam("ldapQuery", "test*")
                .get("/ldap/safeSearch")
                .then()
                .statusCode(200)
                .extract().as(typeRef);
        assertEquals(0, results.size());

        // Verify that non-escaped queries also work with escaped search
        results = RestAssured.given()
                .queryParam("ldapQuery", "test1")
                .get("/ldap/safeSearch")
                .then()
                .statusCode(200)
                .extract().as(typeRef);
        assertEquals(1, results.size());
        assertEquals("test1", results.get(0).get("ou"));
    }

}
