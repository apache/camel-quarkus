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
package org.apache.camel.quarkus.test.support.pqc.certificate.util;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.apache.camel.quarkus.test.support.pqc.certificate.crypto.ChimeraOids;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for validating Chimera hybrid certificates.
 * Validates both RSA and ML-DSA-65 signatures using static methods.
 */
public final class CertificatesUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CertificatesUtil.class);

    public static final Path DEFAULT_CERTS_BASEDIR = Path.of("target/certs");

    private CertificatesUtil() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static Path getCertificatePem(String name) {
        return path(name + "-cert", "pem");
    }

    public static Path getPrimaryKeyPem(String name) {
        return path(name + "-key", "pem");
    }

    public static Path getPqcKeyPem(String name) {
        return path(name + "-pqc-key", "pem");
    }

    public static Path getTruststore(String name) {
        return path(name + "-truststore", "p12");
    }

    public static Path getKeystore(String name) {
        return path(name + "-keystore", "p12");
    }

    private static Path path(String name, String extension) {
        return DEFAULT_CERTS_BASEDIR.resolve(name + "." + extension);
    }

    /**
     * Validates a hybrid certificate by checking both RSA and ML-DSA-65 signatures.
     *
     * @param  cert                           The certificate to validate
     * @throws CertificateValidationException if validation fails
     */
    public static void validateHybridCertificate(X509Certificate cert) throws CertificateValidationException {
        LOG.debug("Validating hybrid certificate for subject: {}", cert.getSubjectX500Principal());

        try {
            // Verify RSA signature (standard X.509 verification)
            if (!verifyRsaSignature(cert)) {
                throw new CertificateValidationException("RSA signature validation failed");
            }

            LOG.debug("RSA signature verified");

            // Verify alternative signature algorithm extension exists
            byte[] altSigAlgExt = cert.getExtensionValue(ChimeraOids.ALT_SIGNATURE_ALGORITHM.getId());
            if (altSigAlgExt == null) {
                throw new CertificateValidationException(
                        "PQC signature algorithm extension missing (OID 2.5.29.73)");
            }

            // Validate it's ML-DSA-65
            ASN1Primitive primitive = ASN1Primitive.fromByteArray(altSigAlgExt);
            byte[] octets = ((ASN1OctetString) primitive).getOctets();
            AlgorithmIdentifier algId = AlgorithmIdentifier.getInstance(octets);

            if (!ChimeraOids.ML_DSA_65.equals(algId.getAlgorithm())) {
                throw new CertificateValidationException(
                        "Expected ML-DSA-65 algorithm OID, found: " + algId.getAlgorithm());
            }

            LOG.debug("ML-DSA-65 algorithm OID validated");

            // Extract and verify ML-DSA-65 signature
            PublicKey mlDsaPublicKey = extractMlDsaPublicKey(cert);
            if (mlDsaPublicKey == null) {
                throw new CertificateValidationException(
                        "PQC public key extension missing (OID 2.5.29.72)");
            }

            byte[] mlDsaSignature = extractMlDsaSignature(cert);
            if (mlDsaSignature == null) {
                throw new CertificateValidationException(
                        "PQC signature extension missing (OID 2.5.29.74)");
            }

            if (!verifyMlDsaSignature(cert, mlDsaPublicKey, mlDsaSignature)) {
                throw new CertificateValidationException("ML-DSA-65 signature validation failed");
            }

            LOG.debug("ML-DSA-65 signature verified - hybrid certificate valid");

        } catch (IOException e) {
            throw new CertificateValidationException("Failed to parse PQC extensions", e);
        } catch (CertificateValidationException e) {
            LOG.warn("Certificate validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            String message = "Unexpected error during certificate validation: " + e.getMessage();
            LOG.error(message, e);
            throw new CertificateValidationException(message, e);
        }
    }

    /**
     * Verifies the RSA signature using standard X.509 verification.
     */
    private static boolean verifyRsaSignature(X509Certificate cert) {
        try {
            // Self-signed certificate - verify with its own public key
            cert.verify(cert.getPublicKey());
            return true;
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | SignatureException
                | NoSuchProviderException e) {
            LOG.error("RSA signature verification failed", e);
            return false;
        }
    }

    /**
     * Extracts the ML-DSA-65 public key from the altSubjectPublicKeyInfo extension.
     */
    private static PublicKey extractMlDsaPublicKey(X509Certificate cert) {
        try {
            byte[] extensionValue = cert.getExtensionValue(ChimeraOids.SUBJECT_ALT_PUBLIC_KEY_INFO.getId());
            if (extensionValue == null) {
                return null;
            }

            // Extension value is wrapped in OCTET STRING
            ASN1Primitive primitive = ASN1Primitive.fromByteArray(extensionValue);
            byte[] octets = ((ASN1OctetString) primitive).getOctets();

            // Parse SubjectPublicKeyInfo
            SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(octets);

            // Convert to PublicKey using X509EncodedKeySpec
            KeyFactory keyFactory = KeyFactory.getInstance("ML-DSA-65", "BC");
            return keyFactory.generatePublic(new X509EncodedKeySpec(spki.getEncoded()));

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
            LOG.error("Failed to extract ML-DSA-65 public key", e);
            return null;
        }
    }

    /**
     * Extracts the ML-DSA-65 signature from the altSignatureValue extension.
     */
    private static byte[] extractMlDsaSignature(X509Certificate cert) {
        try {
            byte[] extensionValue = cert.getExtensionValue(ChimeraOids.ALT_SIGNATURE_VALUE.getId());
            if (extensionValue == null) {
                return null;
            }

            // Extension value is wrapped in OCTET STRING
            ASN1Primitive primitive = ASN1Primitive.fromByteArray(extensionValue);
            byte[] octets = ((ASN1OctetString) primitive).getOctets();

            // Parse as BIT STRING
            ASN1BitString bitString = ASN1BitString.getInstance(octets);
            return bitString.getBytes();

        } catch (IOException e) {
            LOG.error("Failed to extract ML-DSA-65 signature", e);
            return null;
        }
    }

    /**
     * Verifies the ML-DSA-65 signature.
     */
    private static boolean verifyMlDsaSignature(X509Certificate cert, PublicKey pqcKey, byte[] signature) {
        try {
            Signature mlDsaVerify = Signature.getInstance("ML-DSA-65", "BC");
            mlDsaVerify.initVerify(pqcKey);
            mlDsaVerify.update(cert.getSubjectX500Principal().getEncoded());
            return mlDsaVerify.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | NoSuchProviderException e) {
            LOG.error("ML-DSA-65 signature verification failed", e);
            return false;
        }
    }
}
