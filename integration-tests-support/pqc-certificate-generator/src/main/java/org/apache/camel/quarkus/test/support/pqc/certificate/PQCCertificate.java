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

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a single PQC certificate to generate.
 * Used as a nested element within {@link PQCCertificates}.
 *
 * <p>
 * Supports two hybrid modes:
 * <ul>
 * <li><strong>CHIMERA</strong>: Hybrid certificate with both classical (RSA/ECDSA) and PQC signatures.
 * Uses X.509 extensions (Extension.subjectAltPublicKeyInfo, Extension.altSignatureValue) to embed
 * alternative PQC signature alongside primary classical signature. Follows BouncyCastle Almanac page 6.</li>
 * <li><strong>PQC_ONLY</strong>: Pure PQC certificate with only quantum-resistant signature.
 * Uses PQC algorithm as the primary signature algorithm.</li>
 * </ul>
 *
 * <p>
 * Generated files (example for name="server"):
 * <ul>
 * <li>server-cert.pem - X.509 certificate in PEM format</li>
 * <li>server-key.pem - Primary (RSA/ECDSA) private key in PEM format (CHIMERA mode only)</li>
 * <li>server-pqc-key.pem - PQC private key in PEM format</li>
 * <li>server-truststore.p12 - PKCS12 truststore containing the certificate</li>
 * <li>server-keystore.p12 - PKCS12 keystore with primary key and certificate (CHIMERA mode only)</li>
 * </ul>
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface PQCCertificate {

    /**
     * Certificate name prefix (used for file naming).
     * Generated files will be: {name}-cert.pem, {name}-key.pem, etc.
     */
    String name();

    /**
     * Hybrid mode: CHIMERA (RSA/ECDSA + PQC) or PQC_ONLY (pure PQC).
     * Default: CHIMERA (recommended for transition period).
     */
    HybridMode hybridMode() default HybridMode.CHIMERA;

    /**
     * Primary classical algorithm (for TLS handshake compatibility).
     * Used only in CHIMERA mode. Set to NONE for PQC_ONLY mode.
     * Default: RSA_2048.
     */
    PrimaryAlgorithm primaryAlgorithm() default PrimaryAlgorithm.RSA_2048;

    /**
     * Post-Quantum Cryptography algorithm.
     * Used as alternative signature in CHIMERA mode, or primary signature in PQC_ONLY mode.
     * Required.
     */
    PQCAlgorithm pqcAlgorithm();

    /**
     * Certificate Common Name (CN).
     * Default: "localhost".
     */
    String cn() default "localhost";

    /**
     * Subject Alternative Names (SANs).
     * Format: "DNS:example.com" or "IP:127.0.0.1" or just "example.com" (defaults to DNS).
     * Default: empty (no SANs).
     */
    String[] subjectAlternativeNames() default {};

    /**
     * Certificate validity period in days.
     * Default: 30 days.
     */
    int validity() default 2;

    /**
     * Output formats to generate.
     */
    CertificateFormat[] formats();

    /**
     * Password for PKCS12 keystore/truststore.
     */
    String password() default "";
}
