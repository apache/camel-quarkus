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
package org.apache.camel.quarkus.component.hashicorp.vault.it;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class HashicorpVaultClientSSL {
    SSLContext originalContext;

    void init(@Observes StartupEvent event) {
        try (InputStream stream = Files.newInputStream(Paths.get("target/certs/hashicorp-vault-ca.crt"))) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate vaultCertificate = (X509Certificate) certificateFactory.generateCertificate(stream);

            KeyStore vaultKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            vaultKeyStore.load(null, null);
            vaultKeyStore.setCertificateEntry("hashicorp-vault", vaultCertificate);

            TrustManagerFactory vaultTrustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            vaultTrustManagerFactory.init(vaultKeyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, vaultTrustManagerFactory.getTrustManagers(), null);

            originalContext = SSLContext.getDefault();
            SSLContext.setDefault(sslContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void destroy(@Observes ShutdownEvent event) {
        if (originalContext != null) {
            SSLContext.setDefault(originalContext);
        }
    }
}
