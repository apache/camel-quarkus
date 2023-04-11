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
package org.apache.camel.quarkus.component.cxf.soap.securitypolicy.server.it;

import java.io.IOException;
import java.util.Map;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.ws.security.SecurityConstants;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.anyNs;
import static io.restassured.RestAssured.given;

@QuarkusTest
public class CxfWssSecurityPolicyServerTest {

    @Test
    void encrypetdSigned() throws IOException {
        WssSecurityPolicyHelloService client = getPlainClient();

        Map<String, Object> ctx = ((BindingProvider) client).getRequestContext();
        ctx.put(SecurityConstants.CALLBACK_HANDLER, new PasswordCallbackHandler());
        ctx.put(SecurityConstants.SIGNATURE_PROPERTIES,
                Thread.currentThread().getContextClassLoader().getResource("alice.properties"));
        ctx.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        ctx.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        ctx.put(SecurityConstants.ENCRYPT_PROPERTIES,
                Thread.currentThread().getContextClassLoader().getResource("alice.properties"));

        Assertions.assertThat(client.sayHello("foo")).isEqualTo("Secure good morning foo");
    }

    @Test
    void noSecurityConfig() throws IOException {
        WssSecurityPolicyHelloService client = getPlainClient();
        /* Make sure that it fails properly when called without a password */
        Assertions.assertThatExceptionOfType(jakarta.xml.ws.soap.SOAPFaultException.class)
                .isThrownBy(() -> client.sayHello("bar"))
                .withMessage(
                        "A encryption username needs to be declared.");
    }

    @Test
    void unencryptedUnsigned() throws IOException {
        final String SOAP_REQUEST = "<soap:Envelope\n"
                + "        xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "        <soap:Header/>\n"
                + "        <soap:Body>\n"
                + "                <ns2:sayHello\n"
                + "                        xmlns:ns2=\"https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/ws-securitypolicy\"/>\n"
                + "        </soap:Body>\n"
                + "</soap:Envelope>";

        given()
                .body(SOAP_REQUEST)
                .when().post(QuarkusCxfClientTestUtil.getEndpointUrl(getPlainClient()))
                .then()
                .statusCode(500)
                .body(
                        Matchers.containsString(
                                "X509Token: The received token does not match the token inclusion requirement"),
                        Matchers.containsString("Soap Body is not SIGNED"),
                        Matchers.containsString("Soap Body is not ENCRYPTED"));
    }

    @Test
    void fakeSigned() throws IOException {
        /*
         * A syntactically correct signature, however signed with a different certificate with a different CA than
         * deployed on the server
         */
        final String SOAP_REQUEST = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "   <soap:Header>\n"
                + "      <wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" soap:mustUnderstand=\"1\">\n"
                + "         <wsse:BinarySecurityToken EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" wsu:Id=\"X509-53f34474-bcdf-424c-bb90-61b11537cb3f\">MIICnjCCAgmgAwIBAgIEAvQDETALBgkqhkiG9w0BAQUwMzETMBEGA1UEChMKYXBhY2hlLm9yZzEMMAoGA1UECxMDZW5nMQ4wDAYDVQQDEwVjeGZjYTAiGA8yMDIyMDkxOTEyMDA0NFoYDzk5OTkxMjMxMjM1OTU5WjAzMRMwEQYDVQQKEwphcGFjaGUub3JnMQwwCgYDVQQLEwNlbmcxDjAMBgNVBAMTBWFsaWNlMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApBH8SACzUp8ymvPMfQrUVmbvJBfyzOunHVmCDgYN+aTY+if3PBeJc6rdSSTNe7Auyua7HsVLFX/JUFpyKVw7qd21l4TuCA76YHXeRjkLip+uuD/ApKCz1AfJ0QfQ1rnMKcWRm3pla+ONAb2pf2Hz9vbdlDf9R0hQk+Dy7Y4vsH5uRmAw8Sjx8EhkCn54p61qyVJIyp/YZX88AcRUmeOEbwocNbHMAfpKvpsLzNdEfA7fCJcSFBjPrzEALlGiexI3jIQ8LSvXzUPFr8O/NPu4426sYxkB69kgrBd1SJF2FFNm3oiqcVqOY3qMytDdcBOIQPlU6Fro/hcsj8hj3XbaFQIDAQABozswOTAhBgNVHRIEGjAYghZOT1RfRk9SX1BST0RVQ1RJT05fVVNFMBQGA1UdEQQNMAuCCWxvY2FsaG9zdDALBgkqhkiG9w0BAQUDgYEAgkP32oMizt+e5Cv8HZ0WI2XM7YOLav29h9FQCGNNXd8DnXGX4GcAUdkD5FJjiWtgAC2LGcGiY3cA7TPvSTb+o+tLG0masnsq7U0T6X6M/EkOEHh/3IAlLOntYLAK2m1SidWrdcGckxi6ftDbgXfMHgI4GCK0oMMqfCx+NAOFpUY=</wsse:BinarySecurityToken>\n"
                + "         <wsu:Timestamp wsu:Id=\"TS-ccfad22e-b376-4349-a625-8896061d6cfd\">\n"
                + "            <wsu:Created>2022-09-19T12:00:51.226Z</wsu:Created>\n"
                + "            <wsu:Expires>2022-09-19T12:05:51.226Z</wsu:Expires>\n"
                + "         </wsu:Timestamp>\n"
                + "         <xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" Id=\"EK-d77a377b-2dab-4045-ac3e-ea53d7609158\">\n"
                + "            <xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p\" />\n"
                + "            <ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n"
                + "               <wsse:SecurityTokenReference>\n"
                + "                  <ds:X509Data>\n"
                + "                     <ds:X509IssuerSerial>\n"
                + "                        <ds:X509IssuerName>CN=cxfca,OU=eng,O=apache.org</ds:X509IssuerName>\n"
                + "                        <ds:X509SerialNumber>49546002</ds:X509SerialNumber>\n"
                + "                     </ds:X509IssuerSerial>\n"
                + "                  </ds:X509Data>\n"
                + "               </wsse:SecurityTokenReference>\n"
                + "            </ds:KeyInfo>\n"
                + "            <xenc:CipherData>\n"
                + "               <xenc:CipherValue>AaHIPkC7xEAxE1WS8u1lwEp8+VVAFhg61EqUyKAxi4jUPRdYXwa1J7jWQ28Z86preNMLJ7CUfv1stESHCziuZtjoundejV588aN7bymBYwN24zu2LRgUQ49PjADInrMX39gqwv9EtgixEUCwkaCS48/ihY7v7Vb7+cXnrpqAcVrxqF8Y0g2/FfN/k/6IZASGORzHKXcszQM9y+QyDzKZvw9ukSj7oDDBNUhrtrhb2KslOC5UukhSkPQwxrIHAlGCmbTDiohhYjEJCzBE9bb1/+0//7vFx/kPuhr8cRI7wg7WAhl8MIUHvv40TD8FoCo8JH1IJSPfuTs+fcLUSSRMBg==</xenc:CipherValue>\n"
                + "            </xenc:CipherData>\n"
                + "         </xenc:EncryptedKey>\n"
                + "         <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"SIG-d48e583c-d310-428b-9541-1c589934b261\">\n"
                + "            <ds:SignedInfo>\n"
                + "               <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n"
                + "                  <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"soap\" />\n"
                + "               </ds:CanonicalizationMethod>\n"
                + "               <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" />\n"
                + "               <ds:Reference URI=\"#TS-ccfad22e-b376-4349-a625-8896061d6cfd\">\n"
                + "                  <ds:Transforms>\n"
                + "                     <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n"
                + "                        <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"wsse soap\" />\n"
                + "                     </ds:Transform>\n"
                + "                  </ds:Transforms>\n"
                + "                  <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" />\n"
                + "                  <ds:DigestValue>pdXC+DtlPCX25Cb1NipeNUOCwl4=</ds:DigestValue>\n"
                + "               </ds:Reference>\n"
                + "               <ds:Reference URI=\"#_551cb542-d0fe-4fb8-8e11-e0d7dfe47210\">\n"
                + "                  <ds:Transforms>\n"
                + "                     <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" />\n"
                + "                  </ds:Transforms>\n"
                + "                  <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" />\n"
                + "                  <ds:DigestValue>LcxYrZK9si3cDmaUurS3dXHKWEI=</ds:DigestValue>\n"
                + "               </ds:Reference>\n"
                + "               <ds:Reference URI=\"#X509-53f34474-bcdf-424c-bb90-61b11537cb3f\">\n"
                + "                  <ds:Transforms>\n"
                + "                     <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n"
                + "                        <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"soap\" />\n"
                + "                     </ds:Transform>\n"
                + "                  </ds:Transforms>\n"
                + "                  <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" />\n"
                + "                  <ds:DigestValue>vDSN64D/q4EjUCBczlIA0IHzxLc=</ds:DigestValue>\n"
                + "               </ds:Reference>\n"
                + "            </ds:SignedInfo>\n"
                + "            <ds:SignatureValue>d2qb4hRGLF/IOJHDA+hiMpil3hEArczDMEq2Q8bzKy74BbAWu54D2d3JWfMceq3/zHNvG4ND7zOd/0o2TPV05i9k5wVya+FOwlP7ibQ/Yy9lkTNhRMHCL94Es63i7AKPwTcTiGJtYQERcUcp4kXaE/fuRlpD4tv2IfnR+ss1jraOqEtgci//xpjWVUBV4vN5Rpolc10QOYUQGjG/ZqaOgT9QHEnm/WN2ZcFMwOrVlc066ifbbHVnJfS/A+VSjoP2FEnNcmRcG8RrNZc4/LOMVCiCnaldriqTrjtCnF29s0Kwtv00P931Yjb4vPFeLIdw6C+zL8/A3q+TOQ4mH9B4MQ==</ds:SignatureValue>\n"
                + "            <ds:KeyInfo Id=\"KI-2bd212e6-4be5-4d39-b69f-b147f6a540fd\">\n"
                + "               <wsse:SecurityTokenReference wsu:Id=\"STR-4e477765-3f65-4910-93be-9e1fe3d129a0\">\n"
                + "                  <wsse:Reference URI=\"#X509-53f34474-bcdf-424c-bb90-61b11537cb3f\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" />\n"
                + "               </wsse:SecurityTokenReference>\n"
                + "            </ds:KeyInfo>\n"
                + "         </ds:Signature>\n"
                + "         <xenc:ReferenceList xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n"
                + "            <xenc:DataReference URI=\"#ED-ed7e9c30-7b36-452d-ac71-85fa51c60986\" />\n"
                + "         </xenc:ReferenceList>\n"
                + "      </wsse:Security>\n"
                + "   </soap:Header>\n"
                + "   <soap:Body xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" wsu:Id=\"_551cb542-d0fe-4fb8-8e11-e0d7dfe47210\">\n"
                + "      <xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" Id=\"ED-ed7e9c30-7b36-452d-ac71-85fa51c60986\" Type=\"http://www.w3.org/2001/04/xmlenc#Content\">\n"
                + "         <xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\" />\n"
                + "         <ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n"
                + "            <wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsse11=\"http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd\" wsse11:TokenType=\"http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey\">\n"
                + "               <wsse:Reference URI=\"#EK-d77a377b-2dab-4045-ac3e-ea53d7609158\" />\n"
                + "            </wsse:SecurityTokenReference>\n"
                + "         </ds:KeyInfo>\n"
                + "         <xenc:CipherData>\n"
                + "            <xenc:CipherValue>21+PWStmrSWFm9QgE0A6hGMEsTAoKFmhLzX2w7MY2B8KCQNeke5eEW7IBXPQySCNgPw7q1+LmOajM/2AmLsKp2q5ZOXxtOStEkJLxbZ4LCsKtv5rfFiWGTl8d7OS8hHGZLVl1wfEG0n/k7FqCw9WRuKfZFqIsQA06yfQPELVU1hUh1K/vEPGGFol4oa1wzVi</xenc:CipherValue>\n"
                + "         </xenc:CipherData>\n"
                + "      </xenc:EncryptedData>\n"
                + "   </soap:Body>\n"
                + "</soap:Envelope>";

        given()
                .body(SOAP_REQUEST)
                .when().post(QuarkusCxfClientTestUtil.getEndpointUrl(getPlainClient()))
                .then()
                .statusCode(500)
                .body(
                        Matchers.containsString(
                                "A security error was encountered when verifying the message"));
    }

    /**
     * Make sure the policy was included
     */
    @Test
    void wsdl() {
        RestAssuredConfig config = RestAssured.config();
        config.getXmlConfig().namespaceAware(false);
        given()
                .config(config)
                .when().get(QuarkusCxfClientTestUtil.getEndpointUrl(getPlainClient()) + "?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "Policy")
                                        + "/@*[local-name() = 'Id']",
                                CoreMatchers.is("SecurityServiceEncryptThenSignPolicy")));
    }

    WssSecurityPolicyHelloService getPlainClient() {
        return QuarkusCxfClientTestUtil.getClient(
                "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/ws-securitypolicy",
                WssSecurityPolicyHelloService.class,
                "/soapservice/security-policy-hello");
    }
}
