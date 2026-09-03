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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;

import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.net.SSLOptions;
import org.apache.camel.CamelContext;
import org.apache.camel.support.jsse.CipherSuitesParameters;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.NamedGroupsParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.SecureSocketProtocolsParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts Quarkus TlsConfiguration to Camel SSLContextParameters.
 */
final class TlsConfigurationConverter {
    private static final Logger LOG = LoggerFactory.getLogger(TlsConfigurationConverter.class);

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
     * - Handling certificate reloading
     *
     * The {@code protocols}, {@code cipher-suites} and {@code key-exchange-groups} configured via
     * {@code quarkus.tls.*} are copied onto the returned SSLContextParameters and enforced on every SSLEngine and
     * socket factory derived from its SSLContext. Quarkus keeps them on the Vert.x {@link SSLOptions} side and
     * never applies them to the SSLContext itself, so without this they would silently not apply to Camel
     * components.
     *
     * Camel applies an explicitly configured list verbatim, in preference to its own default exclusion filters. Since
     * {@code quarkus.tls.protocols} always carries a value, those default filters never constrain the protocols of a
     * bridged configuration, and they constrain its cipher suites only while {@code quarkus.tls.cipher-suites} is
     * unset. A value JSSE does not recognise is rejected rather than dropped.
     *
     * A certificate revocation list, {@code hostname-verification-algorithm}, SNI, ALPN and
     * {@code pqc-enforcement-policy} have no equivalent in SSLContextParameters. They remain the responsibility of
     * the consuming Camel component, and a warning is logged for the two that weaken verification.
     *
     * @param  tlsConfig the Quarkus TLS configuration
     * @param  name      the configuration name (for logging)
     * @return           SSLContextParameters that delegates to Quarkus's TLS configuration
     */
    static SSLContextParameters convert(TlsConfiguration tlsConfig, String name) {
        SSLContextParameters parameters = new SSLContextParameters() {
            @Override
            public SSLContext createSSLContext(CamelContext camelContext) throws GeneralSecurityException, IOException {
                final SSLContext delegate;
                try {
                    delegate = tlsConfig.createSSLContext();
                } catch (Exception e) {
                    throw new GeneralSecurityException(
                            "Failed to create SSLContext from Quarkus TLS configuration '" + name + "'", e);
                }

                if (camelContext != null) {
                    setCamelContext(camelContext);
                }
                configureSSLContext(delegate);

                // Quarkus builds the SSLContext from key and trust material only. Decorate it the same way
                // SSLContextParameters.createSSLContext does, so that the transport policy carried by this instance
                // is applied to every SSLEngine, SSLSocketFactory and SSLServerSocketFactory derived from it. A
                // plain JSSE SSLContext cannot carry those pins on its own.
                return new SSLContextDecorator(
                        new SSLContextSpiDecorator(
                                delegate,
                                getSSLEngineConfigurers(delegate),
                                getSSLSocketFactoryConfigurers(delegate),
                                getSSLServerSocketFactoryConfigurers(delegate)));
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

        applyTransportPolicy(parameters, tlsConfig, name);

        return parameters;
    }

    /**
     * Copy the transport policy from the Quarkus TLS configuration onto the Camel SSLContextParameters, so that it
     * is enforced on contexts created from it and consumers reading the parameters directly observe the operator's
     * policy. Warn about the configured options that cannot be carried over and that weaken verification if the
     * operator assumes otherwise.
     */
    private static void applyTransportPolicy(SSLContextParameters parameters, TlsConfiguration tlsConfig, String name) {
        SSLOptions sslOptions = tlsConfig.getSSLOptions();
        if (sslOptions != null) {
            Set<String> protocols = sslOptions.getEnabledSecureTransportProtocols();
            if (protocols != null && !protocols.isEmpty()) {
                SecureSocketProtocolsParameters protocolsParameters = new SecureSocketProtocolsParameters();
                protocolsParameters.setSecureSocketProtocol(new ArrayList<>(protocols));
                parameters.setSecureSocketProtocols(protocolsParameters);
                LOG.debug("Quarkus TLS configuration '{}' restricts Camel to secure socket protocols {}", name, protocols);
            }

            Set<String> cipherSuites = sslOptions.getEnabledCipherSuites();
            if (cipherSuites != null && !cipherSuites.isEmpty()) {
                CipherSuitesParameters cipherSuitesParameters = new CipherSuitesParameters();
                cipherSuitesParameters.setCipherSuite(new ArrayList<>(cipherSuites));
                parameters.setCipherSuites(cipherSuitesParameters);
                LOG.debug("Quarkus TLS configuration '{}' restricts Camel to cipher suites {}", name, cipherSuites);
            }

            List<String> keyExchangeGroups = sslOptions.getKeyExchangeGroups();
            if (keyExchangeGroups != null && !keyExchangeGroups.isEmpty()) {
                NamedGroupsParameters namedGroups = new NamedGroupsParameters();
                namedGroups.setNamedGroup(new ArrayList<>(keyExchangeGroups));
                parameters.setNamedGroups(namedGroups);
                LOG.debug("Quarkus TLS configuration '{}' restricts Camel to key exchange groups {}", name,
                        keyExchangeGroups);
            }

            if (!sslOptions.getCrlValues().isEmpty() || !sslOptions.getCrlPaths().isEmpty()) {
                LOG.warn("Quarkus TLS configuration '{}' configures a certificate revocation list, which cannot be "
                        + "carried over to Camel SSLContextParameters. Camel components using this configuration do "
                        + "not check certificate revocation.", name);
            }
        }

        tlsConfig.getHostnameVerificationAlgorithm().ifPresent(algorithm -> LOG.warn(
                "Quarkus TLS configuration '{}' sets hostname-verification-algorithm={}, which cannot be carried over "
                        + "to Camel SSLContextParameters. Hostname verification must be configured on the consuming "
                        + "Camel component or endpoint.",
                name, algorithm));
    }
}
