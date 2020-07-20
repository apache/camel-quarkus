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
package org.apache.camel.quarkus.component.as2.it;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.component.as2.api.AS2Charset;
import org.apache.camel.component.as2.api.AS2ClientConnection;
import org.apache.camel.component.as2.api.AS2ClientManager;
import org.apache.camel.component.as2.api.AS2MediaType;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.camel.component.as2.api.AS2ServerConnection;
import org.apache.camel.component.as2.api.AS2SignatureAlgorithm;
import org.apache.camel.component.as2.api.AS2SignedDataGenerator;
import org.apache.camel.quarkus.component.as2.it.transport.Request;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class As2TestHelper {

    private static final String REQUEST_URI = "/";
    private static final String SUBJECT = "Test Case";
    private static final String AS2_NAME = "878051556";
    private static final String FROM = "mrAS@example.org";
    private static final String EDI_MESSAGE_CONTENT_TRANSFER_ENCODING = "7bit";
    private static final String TARGET_HOST = "localhost";
    private static final String AS2_VERSION = "1.1";
    private static final String USER_AGENT = "Camel AS2 Endpoint";
    private static final String CLIENT_FQDN = "example.org";
    private static final String DISPOSITION_NOTIFICATION_TO = "mrAS@example.org";
    private static final String[] SIGNED_RECEIPT_MIC_ALGORITHMS = new String[] { "sha1", "md5" };
    private static final String SERVER_FQDN = "server.example.com";
    private static final String ORIGIN_SERVER_NAME = "AS2ClientManagerIntegrationTest Server";

    private static AS2ServerConnection serverConnection;
    private static KeyPair serverSigningKP;
    private static List<X509Certificate> serverCertList;
    private static KeyPair signingKP;
    private static X509Certificate signingCert;
    private static List<X509Certificate> certList;
    private static AS2SignedDataGenerator gen;

    private static RequestHandler receiverHandler;

    private As2TestHelper() {
    }

    public static void setup() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        setupKeysAndCertificates();

        // Create and populate certificate store.
        JcaCertStore certs = new JcaCertStore(Collections.emptyList());
        //        JcaCertStore certs = new JcaCertStore(certList);

        // Create capabilities vector
        SMIMECapabilityVector capabilities = new SMIMECapabilityVector();
        capabilities.addCapability(SMIMECapability.dES_EDE3_CBC);
        capabilities.addCapability(SMIMECapability.rC2_CBC, 128);
        capabilities.addCapability(SMIMECapability.dES_CBC);

        // Create signing attributes
        ASN1EncodableVector attributes = new ASN1EncodableVector();
        attributes.add(new SMIMEEncryptionKeyPreferenceAttribute(
                new IssuerAndSerialNumber(new X500Name(signingCert.getIssuerDN().getName()), signingCert.getSerialNumber())));
        attributes.add(new SMIMECapabilitiesAttribute(capabilities));

        for (String signingAlgorithmName : AS2SignedDataGenerator
                .getSupportedSignatureAlgorithmNamesForKey(signingKP.getPrivate())) {
            try {
                gen = new AS2SignedDataGenerator();
                gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BC")
                        .setSignedAttributeGenerator(new AttributeTable(attributes))
                        .build(signingAlgorithmName, signingKP.getPrivate(), signingCert));
                gen.addCertificates(certs);
                break;
            } catch (Exception e) {
                gen = null;
                continue;
            }
        }

        if (gen == null) {
            throw new Exception("failed to create signing generator");
        }
    }

    private static void setupKeysAndCertificates() throws Exception {
        //
        // set up our certificates
        //
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");

        kpg.initialize(1024, new SecureRandom());

        String issueDN = "O=Punkhorn Software, C=US";
        KeyPair issueKP = kpg.generateKeyPair();
        X509Certificate issueCert = makeCertificate(
                issueKP, issueDN, issueKP, issueDN);

        //
        // certificate we sign against
        //
        String signingDN = "CN=William J. Collins, E=punkhornsw@gmail.com, O=Punkhorn Software, C=US";
        signingKP = kpg.generateKeyPair();
        signingCert = makeCertificate(
                signingKP, signingDN, issueKP, issueDN);

        certList = new ArrayList<>();

        certList.add(signingCert);
        certList.add(issueCert);

    }

    /**
     * create a basic X509 certificate from the given keys
     */
    public static X509Certificate makeCertificate(KeyPair subKP, String subDN, KeyPair issKP, String issDN)
            throws GeneralSecurityException, IOException, OperatorCreationException {
        PublicKey subPub = subKP.getPublic();
        PrivateKey issPriv = issKP.getPrivate();
        PublicKey issPub = issKP.getPublic();

        X509v3CertificateBuilder v3CertGen = new JcaX509v3CertificateBuilder(new X500Name(issDN),
                BigInteger.valueOf(serialNo++), new Date(System.currentTimeMillis()),
                new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 100)), new X500Name(subDN), subPub);

        v3CertGen.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyId(subPub));

        v3CertGen.addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyId(issPub));

        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(
                v3CertGen.build(new JcaContentSignerBuilder("MD5withRSA").setProvider("BC").build(issPriv)));
    }

    //
    // certificate serial number seed.
    //
    static int serialNo = 1;

    public static AuthorityKeyIdentifier createAuthorityKeyId(PublicKey pub) throws IOException {
        SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(pub.getEncoded());

        BcX509ExtensionUtils utils = new BcX509ExtensionUtils();
        return utils.createAuthorityKeyIdentifier(info);
    }

    public static SubjectKeyIdentifier createSubjectKeyId(PublicKey pub) throws IOException {
        SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(pub.getEncoded());

        return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
    }

    public static AS2ClientManager createClient(int port) throws IOException {
        AS2ClientConnection clientConnection = new AS2ClientConnection(AS2_VERSION, USER_AGENT, CLIENT_FQDN, TARGET_HOST, port);
        return new AS2ClientManager(clientConnection);
    }

    public static void sendMessage(AS2ClientManager clientManager, String ediMessage) throws HttpException {
        clientManager.send(ediMessage, REQUEST_URI, SUBJECT, FROM, AS2_NAME, AS2_NAME, AS2MessageStructure.PLAIN,
                ContentType.create(AS2MediaType.APPLICATION_EDIFACT, AS2Charset.US_ASCII), null, null, null, null,
                null, DISPOSITION_NOTIFICATION_TO, SIGNED_RECEIPT_MIC_ALGORITHMS, null, null);
    }

    public static Request createClientMessageHeadersPlain() {
        final Map<String, Object> headers = new HashMap<>();
        // parameter type is String
        headers.put("CamelAS2.requestUri", REQUEST_URI);
        // parameter type is String
        headers.put("CamelAS2.subject", SUBJECT);
        // parameter type is String
        headers.put("CamelAS2.from", FROM);
        // parameter type is String
        headers.put("CamelAS2.as2From", AS2_NAME);
        // parameter type is String
        headers.put("CamelAS2.as2To", AS2_NAME);
        // parameter type is org.apache.camel.component.as2.api.AS2MessageStructure
        headers.put("CamelAS2.as2MessageStructure", AS2MessageStructure.PLAIN);
        // parameter type is org.apache.http.entity.ContentType
        headers.put("CamelAS2.ediMessageContentType",
                org.apache.http.entity.ContentType.create(AS2MediaType.APPLICATION_EDIFACT, AS2Charset.US_ASCII));
        // parameter type is String
        headers.put("CamelAS2.ediMessageTransferEncoding", EDI_MESSAGE_CONTENT_TRANSFER_ENCODING);
        // parameter type is String
        headers.put("CamelAS2.dispositionNotificationTo", "mrAS2@example.com");

        return new Request().withHeaders(headers);
    }

    public static Request createClientMessageHeadersEncrypted() {
        final Map<String, Object> headers = new HashMap<>();
        // parameter type is String
        headers.put("CamelAS2.requestUri", REQUEST_URI);
        // parameter type is String
        headers.put("CamelAS2.subject", SUBJECT);
        // parameter type is String
        headers.put("CamelAS2.from", FROM);
        // parameter type is String
        headers.put("CamelAS2.as2From", AS2_NAME);
        // parameter type is String
        headers.put("CamelAS2.as2To", AS2_NAME);
        // parameter type is org.apache.camel.component.as2.api.AS2MessageStructure
        headers.put("CamelAS2.as2MessageStructure", AS2MessageStructure.ENCRYPTED);
        // parameter type is org.apache.http.entity.ContentType
        headers.put("CamelAS2.ediMessageContentType",
                ContentType.create(AS2MediaType.APPLICATION_EDIFACT, AS2Charset.US_ASCII));
        // parameter type is String
        headers.put("CamelAS2.ediMessageTransferEncoding", EDI_MESSAGE_CONTENT_TRANSFER_ENCODING);
        // parameter type is String
        headers.put("CamelAS2.dispositionNotificationTo", "mrAS2@example.com");

        return new Request().withHeaders(headers);
    }

    public static RequestHandler startReceiver(int port) throws Exception {
        if (receiverHandler == null) {
            setupServerKeysAndCertificates();
            receiverHandler = receiveTestMessages(port);
        }
        return receiverHandler;
    }

    private static void setupServerKeysAndCertificates() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        //
        // set up our certificates
        //
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");

        kpg.initialize(1024, new SecureRandom());

        String issueDN = "O=Punkhorn Software, C=US";
        KeyPair issueKP = kpg.generateKeyPair();
        X509Certificate issueCert = As2TestHelper.makeCertificate(
                issueKP, issueDN, issueKP, issueDN);

        //
        // certificate we sign against
        //
        String signingDN = "CN=William J. Collins, E=punkhornsw@gmail.com, O=Punkhorn Software, C=US";
        serverSigningKP = kpg.generateKeyPair();
        X509Certificate signingCert = As2TestHelper.makeCertificate(
                serverSigningKP, signingDN, issueKP, issueDN);

        serverCertList = new ArrayList<>();

        serverCertList.add(signingCert);
        serverCertList.add(issueCert);
    }

    private static RequestHandler receiveTestMessages(int port) throws IOException {
        serverConnection = new AS2ServerConnection(AS2_VERSION, ORIGIN_SERVER_NAME,
                SERVER_FQDN, port, AS2SignatureAlgorithm.SHA256WITHRSA,
                serverCertList.toArray(new Certificate[0]), serverSigningKP.getPrivate(), serverSigningKP.getPrivate());

        RequestHandler handler = new RequestHandler();
        serverConnection.listen("/", handler);

        return handler;
    }

    public static class RequestHandler implements HttpRequestHandler {

        private HttpRequest request;
        private HttpResponse response;

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                throws HttpException, IOException {
            this.request = request;
            this.response = response;
        }

        public HttpRequest getRequest() {
            return request;
        }

        public HttpResponse getResponse() {
            return response;
        }
    }

}
