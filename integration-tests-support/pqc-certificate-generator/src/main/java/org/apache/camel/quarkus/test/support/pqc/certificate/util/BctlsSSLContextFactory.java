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

import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

/**
 * Factory for creating SSLContext using BouncyCastle JSSE provider.
 * This enables better PQC algorithm support compared to standard Java JSSE.
 */
public class BctlsSSLContextFactory {

    private static final String BCJSSE_PROVIDER = "BCJSSE";

    /**
     * Creates an SSLContext using BouncyCastle JSSE provider with custom trust manager.
     * This allows handling of PQC-signed certificates.
     */
    public static SSLContext createSSLContext(TrustManager trustManager) throws Exception {
        // Register BouncyCastle JSSE provider if not already registered
        if (Security.getProvider(BCJSSE_PROVIDER) == null) {
            Security.addProvider(new BouncyCastleJsseProvider());
        }

        // Create SSLContext using BC JSSE provider
        SSLContext sslContext = SSLContext.getInstance("TLS", BCJSSE_PROVIDER);

        // Initialize with custom trust manager
        sslContext.init(null, new TrustManager[] { trustManager }, new SecureRandom());

        return sslContext;
    }

    /**
     * Creates an SSLContext using BouncyCastle JSSE provider with truststore.
     */
    public static SSLContext createSSLContext(KeyStore trustStore, String trustStorePassword) throws Exception {
        // Register BouncyCastle JSSE provider if not already registered
        if (Security.getProvider(BCJSSE_PROVIDER) == null) {
            Security.addProvider(new BouncyCastleJsseProvider());
        }

        // Create trust manager from truststore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm(), BCJSSE_PROVIDER);
        tmf.init(trustStore);

        // Create SSLContext using BC JSSE provider
        SSLContext sslContext = SSLContext.getInstance("TLS", BCJSSE_PROVIDER);
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

        return sslContext;
    }

    /**
     * Creates a trust-all SSLContext for testing PQC connections.
     * Note: This bypasses certificate validation and should only be used in test environments.
     */
    public static SSLContext createTrustAllSSLContext() throws Exception {
        X509TrustManager trustAllManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                // Accept all certificates - allows PQC-signed certs that JSSE can't validate
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
            }
        };

        return createSSLContext(trustAllManager);
    }
}
