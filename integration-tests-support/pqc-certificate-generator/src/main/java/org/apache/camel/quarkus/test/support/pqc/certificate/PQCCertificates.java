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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Declarative annotation for generating PQC (Post-Quantum Cryptography) certificates in integration tests.
 * Generates hybrid RSA/ECDSA+PQC certificates or pure PQC certificates following BouncyCastle Almanac
 * recommendations.
 *
 * <p>
 * Certificates are generated once per test class before tests run (via JUnit5 BeforeAllCallback).
 * Generated files are written to the specified baseDir (default: target/certs).
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * &#64;PQCCertificates(baseDir = "target/certs", certificates = {
 *         &#64;PQCCertificate(name = "server-hybrid", hybridMode = HybridMode.CHIMERA, primaryAlgorithm = PrimaryAlgorithm.RSA_2048, pqcAlgorithm = PQCAlgorithm.DILITHIUM2, cn = "localhost", validity = 30, formats = {
 *                 CertificateFormat.PEM, CertificateFormat.PKCS12 })
 * })
 * &#64;QuarkusTest
 * public class MyTest {
 *     // Certificates generated to:
 *     // - target/certs/server-hybrid-cert.pem
 *     // - target/certs/server-hybrid-key.pem (RSA primary key)
 *     // - target/certs/server-hybrid-pqc-key.pem (Dilithium alternative key)
 *     // - target/certs/server-hybrid-truststore.p12
 *     // - target/certs/server-hybrid-keystore.p12
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(PQCCertificateGenerationExtension.class)
@Inherited
public @interface PQCCertificates {

    /**
     * Base directory for generated certificate files.
     * Defaults to "target/certs" in the module directory.
     */
    String baseDir() default "target/certs";

    /**
     * Whether to replace existing certificates if they exist.
     * Default: false (reuse existing certificates to speed up test runs).
     */
    boolean replaceIfExists() default false;

    /**
     * Whether to resolve Docker host for CN and SANs.
     * If true, uses TestContainers API to detect external Docker host
     * and replaces CN/SANs accordingly (useful for Docker Desktop on Mac/Windows).
     * Default: false.
     */
    boolean docker() default false;

    /**
     * Array of PQC certificates to generate.
     */
    PQCCertificate[] certificates();
}
