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
package org.apache.camel.quarkus.component.xmlsecurity.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.quarkus.test.junit.DisabledOnNativeImage;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.io.IOUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class XmlsecurityTest {

    //@Test
    public void signVerifyEnveloping() throws Exception {
        String signedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(readXMLFile("/xml-test.xml"))
                .post("/xmlsecurity/component/sign/enveloping")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(signedXml.contains("ds:SignatureValue"));

        String verifiedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(signedXml)
                .post("/xmlsecurity/component/verify/enveloping")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertFalse(verifiedXml.contains("ds:SignatureValue"));
    }

    //@Test
    public void signVerifyEnveloped() throws Exception {
        String signedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(readXMLFile("/xml-test.xml"))
                .post("/xmlsecurity/component/sign/enveloped")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(signedXml.contains("ds:SignatureValue"));

        String verifiedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(signedXml)
                .post("/xmlsecurity/component/verify/enveloped")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertFalse(verifiedXml.contains("ds:SignatureValue"));
    }

    //@Test
    public void signVerifyPlainText() throws Exception {
        String signedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(readXMLFile("/xml-test.xml"))
                .post("/xmlsecurity/component/sign/plaintext")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(signedXml.contains("ds:SignatureValue"));

        String verifiedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(signedXml)
                .post("/xmlsecurity/component/verify/plaintext")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertFalse(verifiedXml.contains("ds:SignatureValue"));
    }

    //@Test
    public void signVerifyCanonicalization() throws Exception {
        String signedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(readXMLFile("/xml-test.xml"))
                .post("/xmlsecurity/component/sign/canonicalization")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(signedXml.contains("ds:SignatureValue"));

        String verifiedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(signedXml)
                .post("/xmlsecurity/component/verify/canonicalization")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertFalse(verifiedXml.contains("ds:SignatureValue"));
    }

    //@Test
    public void signVerifySignatureDigestAlgorithm() throws Exception {
        String signedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(readXMLFile("/xml-test.xml"))
                .post("/xmlsecurity/component/sign/signaturedigest")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(signedXml.contains("ds:SignatureValue"));

        String verifiedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(signedXml)
                .post("/xmlsecurity/component/verify/signaturedigest")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertFalse(verifiedXml.contains("ds:SignatureValue"));
    }

    //@Test
    public void signVerifyTransformsXPath() throws Exception {
        String signedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(readXMLFile("/xml-xpath-test.xml"))
                .post("/xmlsecurity/component/sign/transformsxpath")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(signedXml.contains("ds:SignatureValue"));

        String verifiedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(signedXml)
                .post("/xmlsecurity/component/verify/transformsxpath")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertFalse(verifiedXml.contains("ds:SignatureValue"));
    }

    @DisabledOnNativeImage("https://github.com/apache/camel-quarkus/issues/2185")
    //@Test
    public void signVerifyTransformsXsltXPath() throws Exception {
        String signedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(readXMLFile("/xml-test.xml"))
                .post("/xmlsecurity/component/sign/transformsxsltxpath")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(signedXml.contains("ds:SignatureValue"));

        String verifiedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(signedXml)
                .post("/xmlsecurity/component/verify/transformsxsltxpath")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertFalse(verifiedXml.contains("ds:SignatureValue"));
    }

    //@Test
    public void dataformatMarshalUnmarshal() throws Exception {
        String signedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(readXMLFile("/xml-test.xml"))
                .post("/xmlsecurity/dataformat/marshal")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(signedXml.contains("xenc:CipherValue"));

        String unsignedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(signedXml)
                .post("/xmlsecurity/dataformat/unmarshal")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertFalse(unsignedXml.contains("xenc:CipherValue"));
    }

    //@Test
    public void dataformatMarshalUnmarshalPartialContent() throws Exception {
        String signedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(readXMLFile("/xml-test.xml"))
                .post("/xmlsecurity/dataformat/marshal/partial")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(signedXml.contains("<test:child-2><xenc:EncryptedData"));

        String unsignedXml = RestAssured.given()
                .contentType(ContentType.XML)
                .body(signedXml)
                .post("/xmlsecurity/dataformat/unmarshal/partial")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(unsignedXml.contains("<test:child-2>Test 2</test:child-2>"));
    }

    private String readXMLFile(String fileName) throws IOException {
        return IOUtils.toString(XmlsecurityTest.class.getResourceAsStream(fileName), StandardCharsets.UTF_8);
    }
}
