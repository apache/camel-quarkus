
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
package org.apache.camel.quarkus.test.support.pqc.certificate.trustmanager;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.apache.camel.quarkus.test.support.pqc.certificate.util.CertificateValidationException;
import org.apache.camel.quarkus.test.support.pqc.certificate.util.CertificatesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom X509TrustManager that validates hybrid PQC certificates at the TLS layer.
 *
 * This TrustManager validates both RSA and ML-DSA-65 signatures during the TLS handshake,
 * rejecting connections with invalid or RSA-only certificates before the application layer
 * sees the request.
 */
public class HybridPqcX509TrustManager implements X509TrustManager {

    private static final Logger LOG = LoggerFactory.getLogger(HybridPqcX509TrustManager.class);

    private static final String BCPQC_PROVIDER = "BC";
    private static final String BC_PROVIDER = "BC";// Using BC provider for NIST-standardized ML-DSA algorithms

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("Client certificate chain is empty");
        }

        X509Certificate clientCert = chain[0];
        LOG.debug("Validating client certificate at TLS layer: {}", clientCert.getSubjectX500Principal());

        try {
            // Validate hybrid certificate - throws CertificateValidationException on failure
            CertificatesUtil.validateHybridCertificate(clientCert);
            LOG.debug("Client certificate validated successfully at TLS layer (RSA + ML-DSA-65)");
        } catch (CertificateValidationException e) {
            LOG.error("Hybrid PQC certificate validation failed: {}", e.getMessage());
            throw new CertificateException("Validation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("Client certificate chain is empty");
        }

        X509Certificate clientCert = chain[0];
        LOG.debug("Validating server certificate at TLS layer: {}", clientCert.getSubjectX500Principal());

        try {
            // Validate hybrid certificate - throws CertificateValidationException on failure
            CertificatesUtil.validateHybridCertificate(clientCert);
            LOG.debug("Server certificate validated successfully at TLS layer (RSA + ML-DSA-65)");
        } catch (CertificateValidationException e) {
            LOG.error("Hybrid PQC certificate validation failed: {}", e.getMessage());
            throw new CertificateException("Validation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        // Return empty array for self-signed certificates in this demo.
        //
        // In production with a CA hierarchy, this would return the
        // list of trusted CA certificates that can issue client certificates.
        return new X509Certificate[0];
    }
}
