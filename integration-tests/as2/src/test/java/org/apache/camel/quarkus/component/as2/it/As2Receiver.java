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
import java.util.Date;
import java.util.List;

import org.apache.camel.component.as2.api.AS2ServerConnection;
import org.apache.camel.component.as2.api.AS2SignatureAlgorithm;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class As2Receiver {
    private static final String SERVER_FQDN = "server.example.com";
    private static final String ORIGIN_SERVER_NAME = "AS2ClientManagerIntegrationTest Server";
    private static AS2ServerConnection serverConnection;
    private static KeyPair serverSigningKP;
    private static List<X509Certificate> serverCertList;
    private static RequestHandler receiverHandler;
    // certificate serial number seed.
    private static int serialNo = 1;

    private As2Receiver() {
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

        String issueDN = "O=Earth2 Software, C=E2";
        KeyPair issueKP = kpg.generateKeyPair();
        X509Certificate issueCert = makeCertificate(
                issueKP, issueDN, issueKP, issueDN);

        //
        // certificate we sign against
        //
        String signingDN = "CN=Sheldon, E=test@gmail.com, O=Earth2 Software, C=E2";
        serverSigningKP = kpg.generateKeyPair();
        X509Certificate signingCert = makeCertificate(
                serverSigningKP, signingDN, issueKP, issueDN);

        serverCertList = new ArrayList<>();

        serverCertList.add(signingCert);
        serverCertList.add(issueCert);
    }

    private static RequestHandler receiveTestMessages(int port) throws IOException {
        serverConnection = new AS2ServerConnection(As2Helper.AS2_VERSION, ORIGIN_SERVER_NAME,
                SERVER_FQDN, port, AS2SignatureAlgorithm.SHA256WITHRSA,
                serverCertList.toArray(new Certificate[0]), serverSigningKP.getPrivate(), serverSigningKP.getPrivate(), null);

        RequestHandler handler = new RequestHandler();
        serverConnection.listen("/", handler);

        return handler;
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

    public static AuthorityKeyIdentifier createAuthorityKeyId(PublicKey pub) throws IOException {
        SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(pub.getEncoded());

        BcX509ExtensionUtils utils = new BcX509ExtensionUtils();
        return utils.createAuthorityKeyIdentifier(info);
    }

    public static SubjectKeyIdentifier createSubjectKeyId(PublicKey pub) throws IOException {
        SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(pub.getEncoded());

        return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
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
