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
package org.apache.camel.quarkus.component.crypto.it;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.crypto.DigitalSignatureConstants;

@Path("/crypto")
public class CryptoResource {

    public static final String MESSAGE = "Hello Camel Quarkus Crypto";
    private static final String ALIAS = "bob";
    private static final String KEYSTORE = "crypto.jks";
    private static final String KEYSTORE_PASSWORD = "letmein";

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/signature/sign")
    @POST
    public byte[] sign() {
        Exchange exchange = producerTemplate.request("direct:sign", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setBody(MESSAGE);
            }
        });
        Message message = exchange.getMessage();
        return message.getHeader(DigitalSignatureConstants.SIGNATURE, byte[].class);
    }

    @Path("/signature/verify")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response verify(String signature) {
        Exchange exchange = producerTemplate.send("direct:verify", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message message = exchange.getMessage();
                message.setHeader(DigitalSignatureConstants.SIGNATURE, signature);
                message.setBody(MESSAGE);
            }
        });

        if (exchange.isFailed()) {
            // Expected in the signature verification failure scenario
            return Response.serverError().build();
        }

        return Response.ok(exchange.getMessage().getHeaders().size() == 0).build();
    }

    @Path("/encrypt")
    @POST
    public byte[] encryptPayload(String payload) {
        return producerTemplate.requestBody("direct:marshal", payload, byte[].class);
    }

    @Path("/decrypt")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String decryptPgpPayload(byte[] payload) {
        return producerTemplate.requestBody("direct:unmarshal", payload, String.class);
    }

    @Path("/encrypt/pgp")
    @POST
    public byte[] encryptPgpPayload(String payload) {
        return producerTemplate.requestBody("direct:marshalPgp", payload, byte[].class);
    }

    @Path("/decrypt/pgp")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String decryptPayload(byte[] payload) {
        return producerTemplate.requestBody("direct:unmarshalPgp", payload, String.class);
    }

    @jakarta.enterprise.inject.Produces
    public KeyStore keyStore() throws Exception {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = getClass().getResourceAsStream("/" + KEYSTORE)) {
            keystore.load(in, KEYSTORE_PASSWORD.toCharArray());
        }
        return keystore;
    }

    @jakarta.enterprise.inject.Produces
    @Named
    public PrivateKey myPrivateKey(KeyStore keyStore) throws Exception {
        return (PrivateKey) keyStore.getKey(ALIAS, KEYSTORE_PASSWORD.toCharArray());
    }

    @jakarta.enterprise.inject.Produces
    @Named
    public PublicKey myPublicKey(KeyStore keyStore) throws Exception {
        Certificate cert = keyStore.getCertificate(ALIAS);
        return cert.getPublicKey();
    }

    @jakarta.enterprise.inject.Produces
    @Named
    public SecureRandom customSecureRandom() {
        return new SecureRandom();
    }
}
