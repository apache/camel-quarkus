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
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.camel.component.as2.api.AS2ClientConnection;
import org.apache.camel.component.as2.api.AS2ClientManager;
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

public class As2CertificateHelper {

    private static final String TARGET_HOST = "localhost";
    private static final String AS2_VERSION = "1.1";
    private static final String USER_AGENT = "Camel AS2 Endpoint";
    private static final String CLIENT_FQDN = "example.org";
    private static final Duration HTTP_SOCKET_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration HTTP_CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    private static final Integer HTTP_CONNECTION_POOL_SIZE = 5;
    private static final Duration HTTP_CONNECTION_POOL_TTL = Duration.ofMinutes(15);

    private static KeyPair signingKP;
    private static X509Certificate signingCert;
    private static List<X509Certificate> certList;
    // certificate serial number seed.
    static int serialNo = 1;

    private As2CertificateHelper() {
    }

    public static void setup() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        //
        // set up our certificates
        //
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");

        kpg.initialize(1024, new SecureRandom());

        String issueDN = "O=Earth 2 Software, C=E2";
        KeyPair issueKP = kpg.generateKeyPair();
        X509Certificate issueCert = makeCertificate(
                issueKP, issueDN, issueKP, issueDN);

        //
        // certificate we sign against
        //
        String signingDN = "CN=Shekdon, E=test@gmail.com, O=Earth 2 Software, C=E2";
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
        AS2ClientConnection clientConnection = createClientConnection(port);
        return new AS2ClientManager(clientConnection);
    }

    public static AS2ClientConnection createClientConnection(int port) throws IOException {
        AS2ClientConnection clientConnection = new AS2ClientConnection(
                AS2_VERSION,
                USER_AGENT,
                CLIENT_FQDN,
                TARGET_HOST,
                port,
                HTTP_SOCKET_TIMEOUT,
                HTTP_CONNECTION_TIMEOUT,
                HTTP_CONNECTION_POOL_SIZE,
                HTTP_CONNECTION_POOL_TTL);
        return clientConnection;
    }

    public static List<X509Certificate> getCertList() {
        return certList;
    }

    public static KeyPair getSigningKP() {
        return signingKP;
    }
}
