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
package org.apache.camel.quarkus.component.sftp.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.apache.camel.quarkus.test.support.sftp.SftpHostCertTestResource;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;

@TestCertificates(certificates = {
        @Certificate(name = "ftp", formats = {
                Format.PEM }, password = "password"),
        @Certificate(name = "ftp", formats = {
                Format.PKCS12 }, password = "password") })
@QuarkusTest
@QuarkusTestResource(SftpHostCertTestResource.class)
class SftpHostCertTest {

    @Test
    public void testHostCertificateVerification() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Host certificate verification test")
                .post("/sftp/hostcert/create/hostcert-test.txt")
                .then()
                .statusCode(201);

        RestAssured.get("/sftp/hostcert/get/hostcert-test.txt")
                .then()
                .statusCode(200)
                .body(is("Host certificate verification test"));

        RestAssured.delete("/sftp/hostcert/delete/hostcert-test.txt")
                .then()
                .statusCode(204);
    }

    @Test
    public void testHostCertificateVerificationWithCaSignatureAlgorithms() {
        // Test host certificate verification with specific CA signature algorithms
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Host cert with CA algorithms test")
                .post("/sftp/hostcertWithAlgorithms/create/hostcert-algo-test.txt")
                .then()
                .statusCode(201);

        RestAssured.get("/sftp/hostcertWithAlgorithms/get/hostcert-algo-test.txt")
                .then()
                .statusCode(200)
                .body(is("Host cert with CA algorithms test"));

        RestAssured.delete("/sftp/hostcertWithAlgorithms/delete/hostcert-algo-test.txt")
                .then()
                .statusCode(204);
    }
}
