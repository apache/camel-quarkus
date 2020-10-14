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
package org.apache.camel.quarkus.component.lumberjack.it;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;

@ApplicationScoped
public class LumberjackSslService {
    /**
     * Creates SSL Context Parameters for the server
     * 
     * @return
     */
    public SSLContextParameters createServerSSLContextParameters() {
        SSLContextParameters sslContextParameters = new SSLContextParameters();

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        KeyStoreParameters keyStore = new KeyStoreParameters();
        keyStore.setPassword("changeit");
        keyStore.setResource("ssl/keystore.jks");
        keyManagersParameters.setKeyPassword("changeit");
        keyManagersParameters.setKeyStore(keyStore);
        sslContextParameters.setKeyManagers(keyManagersParameters);

        return sslContextParameters;
    }

    /**
     * Creates SSL Context Parameters for the client
     * 
     * @return
     */
    public SSLContextParameters createClientSSLContextParameters() {
        SSLContextParameters sslContextParameters = new SSLContextParameters();

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        KeyStoreParameters trustStore = new KeyStoreParameters();
        trustStore.setPassword("changeit");
        trustStore.setResource("ssl/keystore.jks");
        trustManagersParameters.setKeyStore(trustStore);
        sslContextParameters.setTrustManagers(trustManagersParameters);

        return sslContextParameters;
    }
}
