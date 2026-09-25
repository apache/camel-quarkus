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
package org.apache.camel.quarkus.test.support.pqc.certificate.crypto;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

/**
 * X.509 extension OIDs (Object Identifiers) for Chimera hybrid certificate format.
 *
 * <p>
 * OIDs are globally unique identifiers used in X.509 certificates to label specific
 * data fields and extensions. These OIDs enable standard X.509 certificates to carry
 * both classical (RSA) and post-quantum (ML-DSA-65) cryptographic signatures.
 *
 * <h2>Chimera Hybrid Certificate Structure:</h2>
 *
 * <pre>
 * Standard X.509 Certificate:
 *   Subject: CN=localhost
 *   Public Key: RSA-2048                    ← Classical (readable by all TLS stacks)
 *   Signature: SHA256withRSA                ← Classical signature
 *
 *   Extensions:
 *     [2.5.29.72] altSubjectPublicKeyInfo:  ← ML-DSA-65 public key
 *     [2.5.29.73] altSignatureAlgorithm:    ← Identifies ML-DSA-65 (2.16.840.1.101.3.4.3.18)
 *     [2.5.29.74] altSignatureValue:        ← ML-DSA-65 signature
 * </pre>
 *
 * <p>
 * <b>Why this matters:</b> Both signatures must be valid for authentication.
 * This provides quantum resistance (via ML-DSA-65) while maintaining backward
 * compatibility with standard TLS (via RSA).
 *
 * <h2>OID Hierarchy Explained:</h2>
 * <ul>
 * <li><b>2.5.29.x</b> - X.509 certificate extensions (standardized by ITU-T)
 * <ul>
 * <li>2.5.29.15 - keyUsage (common)</li>
 * <li>2.5.29.17 - subjectAltName (common)</li>
 * <li>2.5.29.72-74 - Chimera hybrid extensions (new for PQC)</li>
 * </ul>
 * </li>
 * <li><b>2.16.840.1.101.3.4.3.18</b> - ML-DSA-65 algorithm (NIST FIPS 204)
 * <ul>
 * <li>2.16.840.1.101.3 - NIST algorithms</li>
 * <li>4.3 - Signature algorithms</li>
 * <li>18 - ML-DSA-65 (Level 3 security)</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc5280">RFC 5280 - X.509 Certificate Standard</a>
 * @see <a href="https://csrc.nist.gov/pubs/fips/204/final">FIPS 204 - ML-DSA Standard</a>
 */
public final class ChimeraOids {

    /**
     * OID 2.5.29.72 - Alternative Subject Public Key Info extension.
     * Contains the ML-DSA-65 public key in addition to the standard RSA public key.
     *
     * <p>
     * This allows a single certificate to carry two public keys:
     * <ul>
     * <li>Primary: RSA-2048 (in standard subjectPublicKeyInfo field)</li>
     * <li>Alternative: ML-DSA-65 (in this extension)</li>
     * </ul>
     *
     * <p>
     * <b>Official References:</b>
     * <ul>
     * <li><a href="https://datatracker.ietf.org/doc/html/draft-ounsworth-pq-composite-sigs-13#section-6.2">
     * IETF Draft: Composite Signatures - Section 6.2 (altSubjectPublicKeyInfo)</a></li>
     * <li><a href="https://oidref.com/2.5.29.72">OID Repository: 2.5.29.72</a></li>
     * <li><a href="https://www.rfc-editor.org/rfc/rfc5280#section-4.2">
     * RFC 5280 - X.509 Certificate Extensions</a></li>
     * </ul>
     */
    public static final ASN1ObjectIdentifier SUBJECT_ALT_PUBLIC_KEY_INFO = new ASN1ObjectIdentifier("2.5.29.72");

    /**
     * OID 2.5.29.73 - Alternative Signature Algorithm extension.
     * Identifies which algorithm was used for the alternative signature (ML-DSA-65).
     *
     * <p>
     * Contains the OID {@link #ML_DSA_65} to specify the PQC algorithm used.
     *
     * <p>
     * <b>Official References:</b>
     * <ul>
     * <li><a href="https://datatracker.ietf.org/doc/html/draft-ounsworth-pq-composite-sigs-13#section-6.1">
     * IETF Draft: Composite Signatures - Section 6.1 (altSignatureAlgorithm)</a></li>
     * <li><a href="https://oidref.com/2.5.29.73">OID Repository: 2.5.29.73</a></li>
     * </ul>
     */
    public static final ASN1ObjectIdentifier ALT_SIGNATURE_ALGORITHM = new ASN1ObjectIdentifier("2.5.29.73");

    /**
     * OID 2.5.29.74 - Alternative Signature Value extension.
     * Contains the actual ML-DSA-65 digital signature bytes.
     *
     * <p>
     * This is the post-quantum signature that proves the certificate is authentic
     * and hasn't been tampered with, computed using the ML-DSA-65 private key.
     *
     * <p>
     * <b>Official References:</b>
     * <ul>
     * <li><a href="https://datatracker.ietf.org/doc/html/draft-ounsworth-pq-composite-sigs-13#section-6.3">
     * IETF Draft: Composite Signatures - Section 6.3 (altSignatureValue)</a></li>
     * <li><a href="https://oidref.com/2.5.29.74">OID Repository: 2.5.29.74</a></li>
     * </ul>
     */
    public static final ASN1ObjectIdentifier ALT_SIGNATURE_VALUE = new ASN1ObjectIdentifier("2.5.29.74");

    /**
     * OID 2.16.840.1.101.3.4.3.18 - ML-DSA-65 algorithm identifier (NIST FIPS 204).
     *
     * <p>
     * ML-DSA-65 (Module-Lattice-Based Digital Signature Algorithm, parameter set 65)
     * is a NIST Level 3 post-quantum signature algorithm resistant to attacks from
     * quantum computers. ML-DSA was standardized from CRYSTALS-Dilithium.
     *
     * <p>
     * <b>Security level:</b> NIST Level 3, equivalent to AES-192 (192-bit security)
     * <br>
     * <b>Signature size:</b> ~3,309 bytes (much larger than RSA-2048's ~256 bytes)
     * <br>
     * <b>Public key size:</b> ~1,952 bytes
     *
     * <p>
     * <b>NOTE:</b> ML-DSA (standardized) and Dilithium (pre-standard) are NOT interoperable.
     * This OID represents the final NIST FIPS 204 standardized version.
     *
     * <p>
     * <b>Official References:</b>
     * <ul>
     * <li><a href="https://csrc.nist.gov/pubs/fips/204/final">
     * NIST FIPS 204 - Module-Lattice-Based Digital Signature Standard (ML-DSA)</a></li>
     * <li><a href="https://datatracker.ietf.org/doc/rfc9881/">
     * RFC 9881 - Algorithm Identifiers for ML-DSA in X.509</a></li>
     * <li><a href="https://oidref.com/2.16.840.1.101.3.4.3.18">
     * OID Repository: 2.16.840.1.101.3.4.3.18</a></li>
     * <li><a href=
     * "https://github.com/bcgit/bc-java/blob/main/core/src/main/java/org/bouncycastle/asn1/nist/NISTObjectIdentifiers.java">
     * BouncyCastle Source: NIST ML-DSA OID definitions</a></li>
     * </ul>
     */
    public static final ASN1ObjectIdentifier ML_DSA_65 = new ASN1ObjectIdentifier("2.16.840.1.101.3.4.3.18");

    private ChimeraOids() {
        throw new AssertionError("Constants class cannot be instantiated");
    }
}
