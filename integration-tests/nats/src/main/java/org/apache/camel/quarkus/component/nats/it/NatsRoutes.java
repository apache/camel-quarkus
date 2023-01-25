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
package org.apache.camel.quarkus.component.nats.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.quarkus.component.nats.it.NatsConfiguration.NATS_ENABLE_TLS_TESTS_CONFIG_KEY;

@ApplicationScoped
public class NatsRoutes extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(NatsRoutes.class);

    @Inject
    NatsResource natsResource;

    @ConfigProperty(name = NATS_ENABLE_TLS_TESTS_CONFIG_KEY)
    boolean tlsTestsEnabled;

    @Override
    public void configure() {
        from("natsBasicAuth:test").routeId("basic-auth").bean(natsResource, "storeMessage");
        from("natsNoAuth:test").routeId("no-auth").bean(natsResource, "storeMessage");
        from("natsTokenAuth:test").routeId("token-auth").bean(natsResource, "storeMessage");

        if (tlsTestsEnabled) {
            LOG.info("TLS tests enabled so starting the TLS auth route");
            final String uri = "natsTlsAuth:test?sslContextParameters=#ssl&secure=true";
            from(uri).routeId("tls-auth").bean(natsResource, "storeMessage");
        } else {
            LOG.info("TLS tests NOT enabled, so NOT starting the TLS auth route");
        }

        from("natsNoAuth:max?maxMessages=2").routeId("2-msg-max").bean(natsResource, "storeMessage");

        String maxMsgUriPattern = "natsNoAuth:qmax?maxMessages=%s&queueName=q";
        fromF(maxMsgUriPattern, 3).routeId("3-qmsg-max").bean(natsResource, "storeMessage");
        fromF(maxMsgUriPattern, 8).routeId("8-qmsg-max").bean(natsResource, "storeMessage");

        from("natsNoAuth:request-reply").setBody().simple("${body} => Reply");
        from("natsNoAuth:reply").routeId("reply").bean(natsResource, "storeMessage");
    }

    @Named("ssl")
    SSLContextParameters createSSLContextParameters() {
        SSLContextParameters sslContextParameters = new SSLContextParameters();

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        KeyStoreParameters keyStore = new KeyStoreParameters();
        keyStore.setPassword("password");
        keyStore.setResource("certs/keystore.jks");
        keyManagersParameters.setKeyPassword("password");
        keyManagersParameters.setKeyStore(keyStore);
        sslContextParameters.setKeyManagers(keyManagersParameters);

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        KeyStoreParameters trustStore = new KeyStoreParameters();
        trustStore.setPassword("password");
        trustStore.setResource("certs/truststore.jks");
        trustManagersParameters.setKeyStore(trustStore);
        sslContextParameters.setTrustManagers(trustManagersParameters);

        return sslContextParameters;
    }
}
