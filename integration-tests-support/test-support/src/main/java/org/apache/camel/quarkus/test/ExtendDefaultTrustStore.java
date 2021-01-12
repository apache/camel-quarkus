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
package org.apache.camel.quarkus.test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * A utility to take the default JVM's trust store, add some custom certificates to it, store it to a
 * temporary location and set {@code javax.net.ssl.trustStore} to the temporary path.
 */
public class ExtendDefaultTrustStore {

    public static Path extendTrustStoreIfNeeded(Path extendedTrustStorePath, String[] certs)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        final Path sslDir = extendedTrustStorePath.getParent();

        if (Files.exists(extendedTrustStorePath)) {
            System.out.println("Nothing to do, the trust store exists already: " + extendedTrustStorePath);
        } else {

            final Path defaultTrustStore = TrustStoreResource.getDefaultTrustStorePath();

            final KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream in = Files.newInputStream(defaultTrustStore)) {
                keystore.load(in, null);
            }
            System.out.println("Loaded " + defaultTrustStore);

            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            for (String cert : certs) {
                System.out.println("Adding " + cert);
                final int colonPos = cert.indexOf(':');
                final String alias = colonPos >= 0 ? cert.substring(0, colonPos) : "localhost";
                final String certPath = colonPos >= 0 ? cert.substring(colonPos + 1) : cert;
                try (InputStream in = new BufferedInputStream(
                        ExtendDefaultTrustStore.class.getClassLoader().getResourceAsStream(certPath))) {
                    final X509Certificate ca = (X509Certificate) cf.generateCertificate(in);
                    keystore.setCertificateEntry(alias, ca);
                }
            }

            Files.createDirectories(sslDir);

            try (OutputStream out = Files.newOutputStream(extendedTrustStorePath)) {
                keystore.store(out, new char[0]);
            }
            System.out.println("Stored the extended trust store to " + extendedTrustStorePath);
        }
        return extendedTrustStorePath;
    }

    public static void main(String[] args) {
        try {
            final Path baseDir = Paths.get(args[0]);
            String[] certs = new String[args.length - 1];
            System.arraycopy(args, 1, certs, 0, args.length - 1);
            extendTrustStoreIfNeeded(baseDir, certs);
        } catch (Exception e) {
            throw new RuntimeException("Could not extend the default trust store with args " + String.join(", ", args), e);
        }
    }
}
