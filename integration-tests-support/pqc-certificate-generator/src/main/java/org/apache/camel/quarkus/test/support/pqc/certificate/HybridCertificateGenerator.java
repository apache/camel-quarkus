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
package org.apache.camel.quarkus.test.support.pqc.certificate;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.apache.camel.quarkus.test.support.pqc.certificate.crypto.ChimeraOids;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.jboss.logging.Logger;

/**
 * Generates hybrid RSA+PQC or pure PQC certificates following BouncyCastle Almanac recommendations.
 * Thread-safe and reusable.
 */
class HybridCertificateGenerator {
    private static final Logger LOG = Logger.getLogger(HybridCertificateGenerator.class);
    private static final String BCPQC_PROVIDER = "BC"; // Using BC provider for NIST-standardized ML-DSA algorithms
    private static final String BC_PROVIDER = "BC";

    static {
        // Ensure BC provider is registered
        if (Security.getProvider(BC_PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        // BCPQC provider kept for backward compatibility but not required for ML-DSA
        if (Security.getProvider("BCPQC") == null) {
            Security.addProvider(new BouncyCastlePQCProvider());
        }
    }

    /**
     * Generates a Chimera-style hybrid certificate with RSA/ECDSA + PQC signatures.
     * Following BC Almanac page 6 recommendations.
     */
    public HybridCertificateFiles generateChimeraCertificate(
            String name,
            PrimaryAlgorithm primaryAlg,
            PQCAlgorithm pqcAlg,
            String cn,
            List<String> sans,
            Duration validity,
            Path outputDir,
            String password) throws Exception {

        LOG.infof("Generating Chimera certificate: name=%s, primary=%s, pqc=%s, cn=%s",
                name, primaryAlg, pqcAlg, cn);

        // Ensure output directory exists
        Files.createDirectories(outputDir);

        // Generate keypairs
        KeyPair primaryKeyPair = generatePrimaryKeyPair(primaryAlg);
        KeyPair pqcKeyPair = generatePQCKeyPair(pqcAlg);

        // Build certificate
        X509Certificate certificate = buildChimeraCertificate(
                primaryKeyPair,
                pqcKeyPair,
                pqcAlg,
                cn,
                sans,
                validity,
                primaryAlg.getSignatureAlgorithm());

        // Export to files
        return exportCertificateFiles(
                name,
                certificate,
                primaryKeyPair,
                pqcKeyPair,
                outputDir,
                password,
                true); // includeFormats: PEM and PKCS12
    }

    /**
     * Generates a pure PQC certificate (no classical algorithm).
     */
    public HybridCertificateFiles generatePurePQCCertificate(
            String name,
            PQCAlgorithm pqcAlg,
            String cn,
            List<String> sans,
            Duration validity,
            Path outputDir,
            String password) throws Exception {

        LOG.infof("Generating pure PQC certificate: name=%s, pqc=%s, cn=%s", name, pqcAlg, cn);

        // Ensure output directory exists
        Files.createDirectories(outputDir);

        // Generate PQC keypair only
        KeyPair pqcKeyPair = generatePQCKeyPair(pqcAlg);

        // Build certificate with PQC only
        X509Certificate certificate = buildPurePQCCertificate(
                pqcKeyPair,
                pqcAlg,
                cn,
                sans,
                validity);

        // Export to files
        return exportCertificateFiles(
                name,
                certificate,
                null, // no primary keypair
                pqcKeyPair,
                outputDir,
                password,
                true);
    }

    private KeyPair generatePrimaryKeyPair(PrimaryAlgorithm alg) throws Exception {
        if (alg == PrimaryAlgorithm.NONE) {
            return null;
        }

        LOG.debugf("Generating primary keypair: %s %d-bit", alg.getAlgorithmName(), alg.getKeySize());

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(alg.getAlgorithmName());
        kpg.initialize(alg.getKeySize());
        return kpg.generateKeyPair();
    }

    private KeyPair generatePQCKeyPair(PQCAlgorithm alg) throws Exception {
        LOG.debugf("Generating PQC keypair: %s", alg.getAlgorithmName());

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(alg.getAlgorithmName(), BCPQC_PROVIDER);
        return kpg.generateKeyPair();
    }

    /**
     * based on example `Ex. 8: ECDSA ML-DSA X.509 Dual Key Certificate Generation` from
     * https://downloads.bouncycastle.org/java/docs/PQC-Almanac.pdf
     */
    private X509Certificate buildChimeraCertificate(
            KeyPair primaryKeyPair,
            KeyPair pqcKeyPair,
            PQCAlgorithm pqcAlg,
            String cn,
            List<String> sans,
            Duration validity,
            String primarySignatureAlg) throws Exception {

        // Certificate validity period
        Instant now = Instant.now();
        Date notBefore = Date.from(now.minus(1, ChronoUnit.HOURS)); // 1 hour buffer for clock skew
        Date notAfter = Date.from(now.plus(validity.toDays(), ChronoUnit.DAYS));

        X500Name subject = new X500Name("CN=" + cn);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        // Build certificate with primary public key (RSA/ECDSA)
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                serialNumber,
                notBefore,
                notAfter,
                subject,
                primaryKeyPair.getPublic());

        // Add Subject Alternative Names if provided
        if (sans != null && !sans.isEmpty()) {
            GeneralName[] names = sans.stream()
                    .map(this::parseSubjectAlternativeName)
                    .toArray(GeneralName[]::new);
            certBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(names));
        }

        // Add PQC public key as alternative public key (Chimera/composite certificate)
        // Following BC Almanac page 6 recommendations
        SubjectPublicKeyInfo pqcPubKeyInfo = SubjectPublicKeyInfo.getInstance(
                pqcKeyPair.getPublic().getEncoded());
        certBuilder.addExtension(ChimeraOids.SUBJECT_ALT_PUBLIC_KEY_INFO, false, pqcPubKeyInfo);

        // Add alternative signature algorithm (Chimera extension)
        AlgorithmIdentifier mlDsaSigAlg = new AlgorithmIdentifier(ChimeraOids.ML_DSA_65);
        certBuilder.addExtension(ChimeraOids.ALT_SIGNATURE_ALGORITHM, false, mlDsaSigAlg);

        // Generate alternative PQC signature
        // For Chimera-style certificates, sign the subject as a marker
        // Full Chimera spec requires signing the actual TBSCertificate bytes
        Signature pqcSig = Signature.getInstance(pqcAlg.getAlgorithmName(), BCPQC_PROVIDER);
        pqcSig.initSign(pqcKeyPair.getPrivate());
        pqcSig.update(subject.getEncoded());
        byte[] pqcSignatureBytes = pqcSig.sign();

        // Add alternative signature value extension
        certBuilder.addExtension(ChimeraOids.ALT_SIGNATURE_VALUE, false,
                new DERBitString(pqcSignatureBytes));

        // Create primary signer (RSA/ECDSA)
        ContentSigner primarySigner = new JcaContentSignerBuilder(primarySignatureAlg)
                .build(primaryKeyPair.getPrivate());

        // Build final certificate with primary signature
        return new JcaX509CertificateConverter()
                .setProvider(BC_PROVIDER)
                .getCertificate(certBuilder.build(primarySigner));
    }

    private X509Certificate buildPurePQCCertificate(
            KeyPair pqcKeyPair,
            PQCAlgorithm pqcAlg,
            String cn,
            List<String> sans,
            Duration validity) throws Exception {

        // Certificate validity period
        Instant now = Instant.now();
        Date notBefore = Date.from(now.minus(1, ChronoUnit.HOURS));
        Date notAfter = Date.from(now.plus(validity.toDays(), ChronoUnit.DAYS));

        X500Name subject = new X500Name("CN=" + cn);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        // Build certificate with PQC public key only
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                serialNumber,
                notBefore,
                notAfter,
                subject,
                pqcKeyPair.getPublic());

        // Add Subject Alternative Names if provided
        if (sans != null && !sans.isEmpty()) {
            GeneralName[] names = sans.stream()
                    .map(this::parseSubjectAlternativeName)
                    .toArray(GeneralName[]::new);
            certBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(names));
        }

        // Create PQC signer
        ContentSigner pqcSigner = new JcaContentSignerBuilder(pqcAlg.getAlgorithmName())
                .setProvider(BCPQC_PROVIDER)
                .build(pqcKeyPair.getPrivate());

        // Build certificate
        return new JcaX509CertificateConverter()
                .setProvider(BC_PROVIDER)
                .getCertificate(certBuilder.build(pqcSigner));
    }

    private HybridCertificateFiles exportCertificateFiles(
            String name,
            X509Certificate certificate,
            KeyPair primaryKeyPair,
            KeyPair pqcKeyPair,
            Path outputDir,
            String password,
            boolean includeFormats) throws Exception {

        // Export certificate to PEM
        Path certPem = outputDir.resolve(name + "-cert.pem");
        try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(certPem.toFile())))) {
            pemWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
        }

        // Export primary key to PEM (if exists)
        Path primaryKeyPem = null;
        if (primaryKeyPair != null) {
            primaryKeyPem = outputDir.resolve(name + "-key.pem");
            try (PemWriter pemWriter = new PemWriter(
                    new OutputStreamWriter(new FileOutputStream(primaryKeyPem.toFile())))) {
                pemWriter.writeObject(new PemObject("PRIVATE KEY", primaryKeyPair.getPrivate().getEncoded()));
            }
        }

        // Export PQC key to PEM
        Path pqcKeyPem = outputDir.resolve(name + "-pqc-key.pem");
        try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(pqcKeyPem.toFile())))) {
            pemWriter.writeObject(new PemObject("PRIVATE KEY", pqcKeyPair.getPrivate().getEncoded()));
        }

        // Create PKCS12 truststore
        Path truststore = outputDir.resolve(name + "-truststore.p12");
        KeyStore ts = KeyStore.getInstance("PKCS12");
        ts.load(null, null);
        ts.setCertificateEntry(name, certificate);
        try (FileOutputStream fos = new FileOutputStream(truststore.toFile())) {
            ts.store(fos, password.toCharArray());
        }

        // Create PKCS12 keystore
        // Use primary key if available (hybrid mode), otherwise use PQC key (pure PQC mode)
        Path keystore = outputDir.resolve(name + "-keystore.p12");
        KeyStore ks = KeyStore.getInstance("PKCS12", BC_PROVIDER);
        ks.load(null, null);
        PrivateKey keyToStore = (primaryKeyPair != null) ? primaryKeyPair.getPrivate() : pqcKeyPair.getPrivate();
        ks.setKeyEntry(name, keyToStore, password.toCharArray(),
                new X509Certificate[] { certificate });
        try (FileOutputStream fos = new FileOutputStream(keystore.toFile())) {
            ks.store(fos, password.toCharArray());
        }

        // Log success with icons
        LOG.infof("⭐  PQC certificate and keys generated successfully!");
        LOG.infof("📜  Certificate: %s", certPem);
        if (primaryKeyPem != null) {
            LOG.infof("🔑  Primary Key: %s", primaryKeyPem);
        }
        LOG.infof("🔐  PQC Key: %s", pqcKeyPem);
        LOG.infof("🔐  Key Store File: %s", keystore);
        LOG.infof("🔓  Trust Store File: %s", truststore);

        return new HybridCertificateFiles(certPem, primaryKeyPem, pqcKeyPem, truststore, keystore);
    }

    private GeneralName parseSubjectAlternativeName(String san) {
        if (san.startsWith("DNS:")) {
            return new GeneralName(GeneralName.dNSName, san.substring(4));
        } else if (san.startsWith("IP:")) {
            return new GeneralName(GeneralName.iPAddress, san.substring(3));
        } else {
            // Default to DNS name
            return new GeneralName(GeneralName.dNSName, san);
        }
    }
}
