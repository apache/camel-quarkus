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
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLOptions;
import org.apache.camel.CamelContext;
import org.apache.camel.support.jsse.CipherSuitesParameters;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.NamedGroupsParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.SecureSocketProtocolsParameters;
import org.apache.camel.support.jsse.TrustAllTrustManager;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts Quarkus TlsConfiguration to Camel SSLContextParameters.
 */
final class TlsConfigurationConverter {
    private static final Logger LOG = LoggerFactory.getLogger(TlsConfigurationConverter.class);
    // The KeyStore API needs a password to hold a private key, and components need it to read the key
    // back, so getKeyManagers() returns it together with the in-memory copy. At least 32 characters for FIPS providers, as Vert.x does
    private static final String KEY_STORE_DUMMY_PASSWORD = "camel-quarkus-tls-registry-in-memory-key-store";

    private TlsConfigurationConverter() {
        // Utility class
    }

    /**
     * Convert a Quarkus TlsConfiguration to Camel SSLContextParameters.
     *
     * This implementation delegates to Quarkus's pre-configured SSLContext and also
     * provides access to the underlying keystores for components that need them
     * (e.g., camel-vertx-http). The key store handed out by {@code getKeyManagers()} is an in-memory copy of the
     * Quarkus key material, see {@link #copyKeyEntries}.
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
     * @param  vertx     the Vert.x instance, used to read the key material of the configuration
     * @return           SSLContextParameters that delegates to Quarkus's TLS configuration
     */
    static SSLContextParameters convert(TlsConfiguration tlsConfig, String name, Vertx vertx) {
        SSLContextParameters parameters = new SSLContextParameters() {
            // Copied on first use, since most components never read it. A certificate reload replaces this whole
            // bean, see TlsRegistryHelper.registerOrUpdateBean
            private KeyStore keyStoreCopy;
            private boolean keyStoreCopied;

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
                KeyStore keyStore = keyStoreCopy();
                if (keyStore == null) {
                    return null;
                }

                KeyManagersParameters kmp = new KeyManagersParameters();
                kmp.setKeyStore(keyStoreParameters(keyStore, KEY_STORE_DUMMY_PASSWORD));
                kmp.setKeyPassword(KEY_STORE_DUMMY_PASSWORD);
                return kmp;
            }

            private synchronized KeyStore keyStoreCopy() {
                if (!keyStoreCopied) {
                    keyStoreCopy = copyKeyEntries(tlsConfig, name, vertx);
                    keyStoreCopied = true;
                }
                return keyStoreCopy;
            }

            @Override
            public TrustManagersParameters getTrustManagers() {
                TrustManagersParameters tmp = new TrustManagersParameters();
                if (tlsConfig.isTrustAll()) {
                    // Carried over as the trust manager, which components honour in preference to a trust store
                    tmp.setTrustManager(TrustAllTrustManager.INSTANCE);
                    return tmp;
                }

                KeyStore trustStore = tlsConfig.getTrustStore();
                if (trustStore == null) {
                    // The component falls back to the JVM default trust store
                    return null;
                }
                tmp.setKeyStore(keyStoreParameters(trustStore, null));
                return tmp;
            }
        };

        applyTransportPolicy(parameters, tlsConfig, name);

        return parameters;
    }

    /**
     * Copy the private keys and certificate chains of a Quarkus TLS configuration into a new in-memory key store
     * protected by {@link #KEY_STORE_DUMMY_PASSWORD}.
     *
     * Components that build their own KeyManagerFactory from {@link SSLContextParameters#getKeyManagers()} need the
     * key password alongside the key store. The password of the key store loaded by Quarkus is not available to
     * Camel: it is the configured password, an alias password, or for PEM an internal Vert.x one. Reading the keys
     * through the key manager that Vert.x builds for the configuration avoids needing it, and keeps the alias
     * selection Vert.x applies when {@code key-store.*.alias} is set.
     *
     * @return the key store, or null when the configuration has no key store
     */
    private static KeyStore copyKeyEntries(TlsConfiguration tlsConfig, String name, Vertx vertx) {
        KeyCertOptions options = tlsConfig.getKeyStoreOptions();
        KeyStore source = tlsConfig.getKeyStore();
        if (options == null || source == null) {
            return null;
        }

        try {
            X509KeyManager keyManager = x509KeyManager(options.getKeyManagerFactory(vertx));

            // The key manager does not list its aliases directly, only the ones matching a key type. It may also name
            // them differently from the source key store, depending on the KeyManagerFactory algorithm
            Set<String> keyTypes = new LinkedHashSet<>();
            for (String alias : Collections.list(source.aliases())) {
                Certificate certificate = source.getCertificate(alias);
                if (source.isKeyEntry(alias) && certificate != null) {
                    keyTypes.add(certificate.getPublicKey().getAlgorithm());
                }
            }

            Set<String> aliases = new LinkedHashSet<>();
            for (String keyType : keyTypes) {
                addAll(aliases, keyManager.getServerAliases(keyType, null));
                addAll(aliases, keyManager.getClientAliases(keyType, null));
            }

            KeyStore copy = KeyStore.getInstance(KeyStore.getDefaultType());
            copy.load(null, null);
            for (String alias : aliases) {
                PrivateKey key = keyManager.getPrivateKey(alias);
                X509Certificate[] chain = keyManager.getCertificateChain(alias);
                if (key != null && chain != null && chain.length > 0) {
                    copy.setKeyEntry(alias, key, KEY_STORE_DUMMY_PASSWORD.toCharArray(), chain);
                }
            }

            if (!keyTypes.isEmpty() && copy.size() == 0) {
                // Fail rather than return no key managers, which would silently connect without a client certificate
                throw new IllegalStateException("No private key could be read from the key store");
            }
            return copy;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read the key material of Quarkus TLS configuration '" + name + "'", e);
        }
    }

    /**
     * Do not override createKeyStore(); the inherited keyStore field shadows captured locals.
     */
    private static KeyStoreParameters keyStoreParameters(KeyStore keyStore, String password) {
        KeyStoreParameters parameters = new KeyStoreParameters();
        parameters.setKeyStore(keyStore);
        parameters.setPassword(password);
        return parameters;
    }

    private static X509KeyManager x509KeyManager(KeyManagerFactory keyManagerFactory) {
        if (keyManagerFactory != null) {
            for (KeyManager keyManager : keyManagerFactory.getKeyManagers()) {
                if (keyManager instanceof X509KeyManager x509KeyManager) {
                    return x509KeyManager;
                }
            }
        }
        throw new IllegalStateException("No X509KeyManager available for the key store");
    }

    private static void addAll(Set<String> target, String[] values) {
        if (values != null) {
            Collections.addAll(target, values);
        }
    }

    /**
     * Copy the transport policy from the Quarkus TLS configuration onto the Camel SSLContextParameters, so that it
     * is enforced on contexts created from it and consumers reading the parameters directly observe the operator's
     * policy. Warn about the configured options that cannot be carried over and that weaken verification if the
     * operator assumes otherwise.
     */
    private static void applyTransportPolicy(SSLContextParameters parameters, TlsConfiguration tlsConfig, String name) {
        // Protocols, cipher suites, key exchange groups and CRLs are populated identically on the client and server
        // options, so either can be read here
        SSLOptions sslOptions = tlsConfig.getClientSSLOptions();
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
