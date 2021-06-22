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
package org.apache.camel.quarkus.component.http.it;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Named;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;

public class HttpRoute extends RouteBuilder {
    @Override
    public void configure() {
        from("netty-http:http://0.0.0.0:{{camel.netty-http.test-port}}/test/server/hello")
                .transform().constant("Netty Hello World");

        from("netty-http:http://0.0.0.0:{{camel.netty-http.compression-test-port}}/compressed?compression=true")
                .transform().constant("Netty Hello World Compressed");

        from("netty-http:http://0.0.0.0:{{camel.netty-http.https-test-port}}/countries/cz?ssl=true&sslContextParameters=#sslContextParameters")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        try (InputStream stream = HttpRoute.class.getResourceAsStream("/restcountries/cz.json")) {
                            String json = exchange.getContext().getTypeConverter().convertTo(String.class, stream);
                            exchange.getMessage().setBody(json);
                        }
                    }
                });
    }

    @Named
    public SSLContextParameters sslContextParameters() {
        KeyStoreParameters keystoreParameters = new KeyStoreParameters();
        keystoreParameters.setResource("/jsse/keystore.p12");
        keystoreParameters.setPassword("changeit");

        KeyStoreParameters truststoreParameters = new KeyStoreParameters();
        truststoreParameters.setResource("/jsse/truststore.jks");
        truststoreParameters.setPassword("changeit");

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setKeyStore(truststoreParameters);
        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        keyManagersParameters.setKeyPassword("changeit");
        keyManagersParameters.setKeyStore(keystoreParameters);
        sslContextParameters.setKeyManagers(keyManagersParameters);

        return sslContextParameters;
    }

    private static byte[] readStore(String path) throws IOException {
        byte[] data = null;
        final InputStream resource = HttpRoute.class.getResourceAsStream(path);
        if (resource != null) {
            try (InputStream is = resource) {
                data = inputStreamToBytes(is);
            }
        }
        return data;
    }

    private static byte[] inputStreamToBytes(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = is.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

}
