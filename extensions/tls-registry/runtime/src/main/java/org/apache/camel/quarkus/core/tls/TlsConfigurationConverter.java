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
package org.apache.camel.quarkus.core.tls;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import io.quarkus.tls.TlsConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;

/**
 * Converts Quarkus TlsConfiguration to Camel SSLContextParameters.
 */
final class TlsConfigurationConverter {
    private TlsConfigurationConverter() {
        // Utility class
    }

    /**
     * Convert a Quarkus TlsConfiguration to Camel SSLContextParameters.
     *
     * This implementation delegates to Quarkus's pre-configured SSLContext and also
     * provides access to the underlying keystores for components that need them
     * (e.g., camel-vertx-http).
     *
     * The Quarkus TLS registry handles all the complexity of:
     *
     * - Loading keystores and truststores (JKS, PKCS12, PEM)
     * - Configuring key and trust managers
     * - Setting cipher suites and protocols
     * - Handling certificate reloading
     *
     * @param  tlsConfig the Quarkus TLS configuration
     * @param  name      the configuration name (for logging)
     * @return           SSLContextParameters that delegates to Quarkus's TLS configuration
     */
    static SSLContextParameters convert(TlsConfiguration tlsConfig, String name) {
        return new SSLContextParameters() {
            @Override
            public SSLContext createSSLContext(CamelContext camelContext) throws GeneralSecurityException, IOException {
                try {
                    return tlsConfig.createSSLContext();
                } catch (Exception e) {
                    throw new GeneralSecurityException(
                            "Failed to create SSLContext from Quarkus TLS configuration '" + name + "'", e);
                }
            }

            @Override
            public KeyManagersParameters getKeyManagers() {
                KeyStore keyStore = tlsConfig.getKeyStore();
                if (keyStore == null) {
                    return null;
                }

                // Create a KeyManagersParameters that wraps the Quarkus keystore
                KeyManagersParameters kmp = new KeyManagersParameters();

                // Create KeyStoreParameters wrapping the pre-loaded Quarkus keystore
                KeyStoreParameters ksp = new KeyStoreParameters() {
                    @Override
                    public KeyStore createKeyStore() {
                        // Return the already-loaded Quarkus keystore
                        return keyStore;
                    }
                };
                kmp.setKeyStore(ksp);

                return kmp;
            }

            @Override
            public TrustManagersParameters getTrustManagers() {
                KeyStore trustStore = tlsConfig.getTrustStore();

                // If no truststore and not trust-all mode, return null
                // This is valid for scenarios where no server verification is needed
                if (trustStore == null && !tlsConfig.isTrustAll()) {
                    return null;
                }

                // If trust-all is enabled, return null and let the SSLContext handle it
                // The SSLContext from Quarkus is already configured for trust-all
                if (tlsConfig.isTrustAll()) {
                    return null;
                }

                // Create a TrustManagersParameters that wraps the Quarkus truststore
                TrustManagersParameters tmp = new TrustManagersParameters();

                // Create KeyStoreParameters wrapping the pre-loaded Quarkus truststore
                KeyStoreParameters ksp = new KeyStoreParameters() {
                    @Override
                    public KeyStore createKeyStore() {
                        // Return the already-loaded Quarkus truststore
                        return trustStore;
                    }
                };
                tmp.setKeyStore(ksp);

                return tmp;
            }
        };
    }
}
