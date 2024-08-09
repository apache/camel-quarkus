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
package org.apache.camel.quarkus.component.splunk.hec.it;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.splunkhec.SplunkHECConstants;
import org.apache.camel.quarkus.test.support.splunk.SplunkConstants;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/splunk-hec")
@ApplicationScoped
public class SplunkHecResource {

    @Inject
    ProducerTemplate producer;

    @ConfigProperty(name = SplunkConstants.PARAM_HEC_PORT)
    Integer hecPort;

    @ConfigProperty(name = SplunkConstants.PARAM_REMOTE_HOST)
    String host;

    @ConfigProperty(name = SplunkConstants.PARAM_TEST_INDEX)
    String index;

    @ConfigProperty(name = SplunkConstants.PARAM_HEC_TOKEN)
    String token;

    @Path("/send/{sslContextParameters}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response send(String data,
            @PathParam("sslContextParameters") String sslContextParameters,
            @QueryParam("indexTime") Long indexTime) {
        String url = String.format(
                "splunk-hec:%s:%s?token=%s&sslContextParameters=#%s&skipTlsVerify=false&https=true&index=%s",
                host, hecPort, token, sslContextParameters, index);
        try {
            return Response.status(200)
                    .entity(producer.requestBodyAndHeader(url, data, SplunkHECConstants.INDEX_TIME, indexTime, String.class))
                    .build();
        } catch (Exception e) {
            if (e.getCause() instanceof SSLException) {
                return Response.status(500).entity(e.getCause().getMessage()).build();
            }
            throw new RuntimeException(e);
        }
    }

    @Named("sslContextParameters")
    public SSLContextParameters createServerSSLContextParameters() {
        return createServerSSLContextParameters("target/certs/splunkca.jks");
    }

    /**
     * Creates SSL Context Parameters for the server
     *
     * @return
     */
    @Named("wrongSslContextParameters")
    public SSLContextParameters createWrongServerSSLContextParameters() {
        return createServerSSLContextParameters("target/certs/wrong-splunkca.jks");
    }

    private SSLContextParameters createServerSSLContextParameters(String keystore) {
        return new SSLContextParameters() {
            @Override
            public SSLContext createSSLContext(CamelContext camelContext) throws GeneralSecurityException, IOException {

                ///bealdung https://www.baeldung.com/java-custom-truststore
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init((KeyStore) null);

                try (FileInputStream myKeys = new FileInputStream(
                        Paths.get(keystore).toFile())) {
                    KeyStore myTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    myTrustStore.load(myKeys, "password".toCharArray());
                    trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init(myTrustStore);

                    X509TrustManager myTrustManager = null;
                    for (TrustManager tm : trustManagerFactory.getTrustManagers()) {
                        if (tm instanceof X509TrustManager x509TrustManager) {
                            myTrustManager = x509TrustManager;
                            break;
                        }
                    }

                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(null, new TrustManager[] { myTrustManager }, null);
                    return context;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };
    }
}
