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
package org.apache.camel.quarkus.component.ssh.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.component.ssh.SshConstants;
import org.apache.camel.quarkus.test.DisabledIfFipsMode;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestCertificates(certificates = {
        @Certificate(name = "user01", formats = {
                Format.PEM }, password = "changeit"),
        @Certificate(name = "eddsa", formats = {
                Format.PEM }, password = "changeit") })
@QuarkusTest
@QuarkusTestResource(SshTestResource.class)
class SshTest {

    @Test
    public void testWriteToSSHAndReadFromSSH() {
        final String fileContent = "Hello Camel Quarkus SSH";
        // Write a file to SSH session
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(fileContent)
                .post("/ssh/file/camelTest")
                .then()
                .statusCode(201);

        // Retrieve a file from SSH session
        String sshFileContent = RestAssured.get("/ssh/file/camelTest")
                .then()
                .contentType(ContentType.TEXT)
                .statusCode(200)
                .extract()
                .body().asString();

        assertEquals(fileContent, sshFileContent);
    }

    @Test
    public void testHeaders() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(SshConstants.USERNAME_HEADER, SshTestResource.USERNAME, SshConstants.PASSWORD_HEADER,
                        SshTestResource.PASSWORD))
                .queryParam("command", "wrong")
                .post("/ssh/send/")
                .then()
                .statusCode(200)
                .body("", Matchers.hasEntry(SshConstants.EXIT_VALUE, "127"))
                .body("", Matchers.hasEntry(Matchers.matchesRegex(SshConstants.STDERR),
                        Matchers.containsString("command not found")));
    }

    @Test
    public void testProducerInRoute() {
        RestAssured.given()
                .body("echo Hello World")
                .post("/ssh/sendToDirect/exampleProducer")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello World"));
    }

    @Test
    public void testKeyProvider() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("component", "ssh-with-key-provider")
                .queryParam("command", "echo test")
                .queryParam("serverType", "user01Key")
                .post("/ssh/send")
                .then()
                .statusCode(200)
                .body("", Matchers.hasEntry(SshConstants.EXIT_VALUE, "0"))
                //expecting error from command factory
                .body("", Matchers.hasEntry(SshConstants.STDERR, "Expected Error:echo test"));
    }

    @Test
    public void testCertificate() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("component", "ssh-cert")
                .queryParam("command", "echo test")
                .queryParam("serverType", "user01Key")
                .queryParam("pathSuffix", "certResource=file:target/certs/user01.key&certResourcePassword=changeit")
                .post("/ssh/send")
                .then()
                .statusCode(200)
                .body("", Matchers.hasEntry(SshConstants.EXIT_VALUE, "0"))
                //expecting error from command factory
                .body("", Matchers.hasEntry(SshConstants.STDERR, "Expected Error:echo test"));
    }

    @DisabledIfFipsMode //ED25519 keys are not allowed in FIPS mode
    @Test
    public void testProducerWithEdDSAKeyType() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("command", "echo Hello!")
                .queryParam("serverType", "edKey")
                .queryParam("pathSuffix",
                        "timeout=3000&knownHostsResource=/edDSA/known_hosts&failOnUnknownHost=true")
                .body(Map.of(SshConstants.USERNAME_HEADER, SshTestResource.USERNAME, SshConstants.PASSWORD_HEADER,
                        SshTestResource.PASSWORD))
                .post("/ssh/send")
                .then()
                .statusCode(200)
                .body("", Matchers.hasEntry(SshConstants.EXIT_VALUE, "0"))
                //expecting error from command factory
                .body("", Matchers.hasEntry(SshConstants.STDERR, "Expected Error:echo Hello!"));
    }

}
