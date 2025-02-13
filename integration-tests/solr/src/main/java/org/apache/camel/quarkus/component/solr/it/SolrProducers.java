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
package org.apache.camel.quarkus.component.solr.it;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SolrProducers {
    public static final String KEYSTORE_PASSWORD = "secret";

    @ConfigProperty(name = "solr.host")
    String solrHost;

    @Singleton
    SolrClient solrClient(SSLContext sslContext) {
        return new HttpJdkSolrClient.Builder(solrHost)
                .withSSLContext(sslContext)
                .build();
    }

    @Singleton
    SSLContext sslContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream keyStoreInput = new FileInputStream("target/certs/solr-keystore.p12")) {
            keyStore.load(keyStoreInput, KEYSTORE_PASSWORD.toCharArray());
        }

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream trustStoreInput = new FileInputStream("target/certs/solr-truststore.p12")) {
            trustStore.load(trustStoreInput, KEYSTORE_PASSWORD.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext;
    }
}
