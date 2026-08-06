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
package org.apache.camel.quarkus.test.support.pqc.certificate.client;

import javax.net.ssl.SSLContext;

import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;

/**
 * HttpClient configurer that uses BouncyCastle TLS (BCTLS) for PQC support.
 */
public class PqcSslClientConfigurer implements HttpClientConfigurer {

    private final SSLContext sslContext;

    public PqcSslClientConfigurer(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    public void configureHttpClient(HttpClientBuilder clientBuilder) {
        try {
            // Create SSL socket factory with BCTLS SSLContext
            // Use ALLOW_ALL_HOSTNAME_VERIFIER for testing (accepts any hostname)
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    new String[] { "TLSv1.3", "TLSv1.2" }, // Supported protocols
                    null, // Use default cipher suites from BCTLS
                    (hostname, session) -> true); // Accept all hostnames for testing

            // Create connection manager with custom SSL socket factory
            HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();

            clientBuilder.setConnectionManager(connectionManager);
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure HttpClient with BCTLS", e);
        }
    }
}
