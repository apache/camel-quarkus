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
package org.apache.camel.quarkus.component.platform.http.proxy.it;

import java.net.URI;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Routes extends RouteBuilder {

    @ConfigProperty(name = "platform.origin.url.ssl")
    Optional<String> sslUrl;

    @Named
    @Produces
    @Singleton
    public SSLContextParameters sslContextParameters() {
        KeyStoreParameters keyStore = new KeyStoreParameters();
        keyStore.setPassword("changeit");
        keyStore.setResource("file://target/certs/proxy-ssl-keystore.p12");

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        keyManagersParameters.setKeyPassword("changeit");
        keyManagersParameters.setKeyStore(keyStore);

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setKeyStore(keyStore);

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(keyManagersParameters);
        sslContextParameters.setTrustManagers(trustManagersParameters);
        return sslContextParameters;
    }

    @Named
    public NoopHostnameVerifier x509HostnameVerifier() {
        return NoopHostnameVerifier.INSTANCE;
    }

    @Override
    public void configure() throws Exception {
        // Requests addressed to the origin HTTPS port are forwarded over TLS, everything else over plain HTTP
        String sslHost = sslUrl.map(url -> URI.create(url).getAuthority()).orElse("");

        from("platform-http:proxy")
                .log("header is :: ${headers.CamelHttpHost}")
                .choice()
                .when(header(Exchange.HTTP_HOST).isEqualTo(sslHost))
                .toD("https://${headers." + Exchange.HTTP_HOST
                        + "}?bridgeEndpoint=true&sslContextParameters=#sslContextParameters&x509HostnameVerifier=#x509HostnameVerifier")
                .otherwise()
                .toD("http://${headers." + Exchange.HTTP_HOST + "}?bridgeEndpoint=true");
    }
}
